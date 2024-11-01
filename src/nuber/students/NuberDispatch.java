package nuber.students;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The core Dispatch class that instantiates and manages everything for Nuber
 * 
 * @author james
 *
 */
public class NuberDispatch {

	/**
	 * The maximum number of idle drivers that can be awaiting a booking 
	 */
    private final int MAX_DRIVERS = 999;
    private final boolean logEvents;
    private final Map<String, NuberRegion> regions = new HashMap<>();
    private final BlockingQueue<Driver> idleDrivers = new LinkedBlockingQueue<>(MAX_DRIVERS);
    private final AtomicInteger bookingsAwaitingDriver = new AtomicInteger(0);
    private volatile boolean shutdown = false;
	
	/**
	 * Creates a new dispatch objects and instantiates the required regions and any other objects required.
	 * It should be able to handle a variable number of regions based on the HashMap provided.
	 * 
	 * @param regionInfo Map of region names and the max simultaneous bookings they can handle
	 * @param logEvents Whether logEvent should print out events passed to it
	 */
    public NuberDispatch(HashMap<String, Integer> regionInfo, boolean logEvents) {
        this.logEvents = logEvents;
        logEvent(null, "Creating Nuber Dispatch");
        regionInfo.forEach((name, maxJobs) -> {
            regions.put(name, new NuberRegion(this, name, maxJobs));
            logEvent(null, "Creating Nuber region for " + name);
        });
        logEvent(null, "Done creating " + regions.size() + " regions");
    }

    
	/**
	 * Adds drivers to a queue of idle driver.
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @param The driver to add to the queue.
	 * @return Returns true if driver was added to the queue
	 */
    public boolean addDriver(Driver newDriver) {   
        boolean added = idleDrivers.offer(newDriver);
        if (!added) {
            logEvent(null, "Driver queue is full; unable to add driver: " + newDriver.name);
        }
        return added;
    }
	
	/**
	 * Gets a driver from the front of the queue
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @return A driver that has been removed from the queue
	 */
    public Driver getDriver(long timeoutMillis) throws InterruptedException {
        Driver driver = idleDrivers.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        if (driver == null) {
            logEvent(null, "No driver available within timeout of " + timeoutMillis + " ms");
        }
        return driver;
    }
	
	/**
	 * Prints out the string
	 * 	    booking + ": " + message
	 * to the standard output only if the logEvents variable passed into the constructor was true
	 * 
	 * @param booking The booking that's responsible for the event occurring
	 * @param message The message to show
	 */
    public void logEvent(Booking booking, String message) {
        if (!logEvents) return;
        if (booking == null) {
            System.out.println("System: " + message);
        } else {
            System.out.println(booking + ": " + message);
        }
    }


	/**
	 * Books a given passenger into a given Nuber region.
	 * 
	 * Once a passenger is booked, the getBookingsAwaitingDriver() should be returning one higher.
	 * 
	 * If the region has been asked to shutdown, the booking should be rejected, and null returned.
	 * 
	 * @param passenger The passenger to book
	 * @param region The region to book them into
	 * @return returns a Future<BookingResult> object
	 */
    public Future<BookingResult> bookPassenger(Passenger passenger, String regionName) {
        if (shutdown) {
            Booking tempBooking = new Booking(this, passenger);
            logEvent(tempBooking, "Booking rejected: Region is shutting down.");
            return null;
        }

        NuberRegion region = regions.get(regionName);
        if (region != null) {
            return region.bookPassenger(passenger);
        } else {
            logEvent(null, "Region " + regionName + " not found for booking.");
            return null;
        }
    }
    
    // Method to increment pending bookings
    public synchronized void incrementBookingsAwaitingDriver() {
        bookingsAwaitingDriver.incrementAndGet();

    }
    // Method to decrement pending bookings
    public synchronized void decrementBookingsAwaitingDriver() {
        if (bookingsAwaitingDriver.get() > 0) {
            bookingsAwaitingDriver.decrementAndGet();
        }
    }
	/**
	 * Gets the number of non-completed bookings that are awaiting a driver from dispatch
	 * 
	 * Once a driver is given to a booking, the value in this counter should be reduced by one
	 * 
	 * @return Number of bookings awaiting driver, across ALL regions
	 */
    public int getBookingsAwaitingDriver() {
        return bookingsAwaitingDriver.get();
    }
	
	/**
	 * Tells all regions to finish existing bookings already allocated, and stop accepting new bookings
	 */
    public void shutdown() {
        shutdown = true;
        regions.values().forEach(NuberRegion::shutdown);
        logEvent(null, "Dispatch shutdown complete.");
    }
    
    public void awaitTermination() {
        regions.values().forEach(region -> {
            try {
                region.awaitTermination();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Error waiting for region termination: " + e.getMessage());
            }
        });
    }
    }
