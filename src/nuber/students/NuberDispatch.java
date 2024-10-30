package nuber.students;

import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * The core Dispatch class that instantiates and manages everything for Nuber
 * 
 * @author james
 *
 */
public class NuberDispatch {
    private final BlockingQueue<Driver> availableDrivers;
    private final HashMap<String, NuberRegion> regions;
	/**
	 * The maximum number of idle drivers that can be awaiting a booking 
	 */
	private final int MAX_DRIVERS = 999;
	
	private boolean logEvents = false;
	
	/**
	 * Creates a new dispatch objects and instantiates the required regions and any other objects required.
	 * It should be able to handle a variable number of regions based on the HashMap provided.
	 * 
	 * @param regionInfo Map of region names and the max simultaneous bookings they can handle
	 * @param logEvents Whether logEvent should print out events passed to it
	 */
	public NuberDispatch(HashMap<String, Integer> regionInfo, boolean logEvents)
	{
		this.availableDrivers = new LinkedBlockingQueue<>(MAX_DRIVERS);
		this.regions = new HashMap<>();
        this.logEvents = logEvents;
        
        for (String regionName : regionInfo.keySet()) {
            int maxSimultaneousJobs = regionInfo.get(regionName);
            regions.put(regionName, new NuberRegion(this, regionName, maxSimultaneousJobs));
        }

        System.out.println("Creating Nuber Dispatch");
    }
	
    public synchronized int getTotalActiveBookings1() {
        int totalActive = 0;
        for (NuberRegion region : regions.values()) {
            totalActive += region.getActiveBookingsCount();
        }
        return totalActive;
    }
    
    public synchronized int getTotalPendingBookings1() {
        int totalPending = 0;
        for (NuberRegion region : regions.values()) {
            totalPending += region.getPendingBookingsCount();
        }
        return totalPending;
    }
        
	/**
	 * Adds drivers to a queue of idle driver.
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @param The driver to add to the queue.
	 * @return Returns true if driver was added to the queue
	 */
	public boolean addDriver(Driver newDriver)
	{
		try {
	        availableDrivers.put(newDriver);
	        return true;
	    } catch (InterruptedException e) {
	    	Thread.currentThread().interrupt();
	        return false;
	    }
	}
	
	/**
	 * Gets a driver from the front of the queue
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @return A driver that has been removed from the queue
	 */
	public Driver getDriver()
	{
	    try {
	    	return availableDrivers.take();
	    } catch (InterruptedException e) {
	    	Thread.currentThread().interrupt();
	    	return null;
	    }
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
		
		System.out.println(booking + ": " + message);
		
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
        NuberRegion region = regions.get(regionName);

        if (region == null) {
            System.err.println("Region " + regionName + " does not exist.");
            return null;
        }

        Future<BookingResult> bookingFuture = region.bookPassenger(passenger);
        if (bookingFuture == null) {
            System.out.println("Booking rejected for passenger " + passenger.name + " in region " + regionName);
        }
        
        return bookingFuture;

	}
	
    public synchronized int getTotalActiveBookings() {
        int totalActive = 0;
        for (NuberRegion region : regions.values()) {
            totalActive += region.getActiveBookingsCount();
        }
        return totalActive;
    }
    
    public synchronized int getTotalPendingBookings() {
        int totalPending = 0;
        for (NuberRegion region : regions.values()) {
            totalPending += region.getPendingBookingsCount();
        }
        return totalPending;
    }

	/**
	 * Gets the number of non-completed bookings that are awaiting a driver from dispatch
	 * 
	 * Once a driver is given to a booking, the value in this counter should be reduced by one
	 * 
	 * @return Number of bookings awaiting driver, across ALL regions
	 */
    public synchronized int getBookingsAwaitingDriver() {
        int totalAwaitingDrivers = 0;
        for (NuberRegion region : regions.values()) {
            totalAwaitingDrivers += region.getPendingBookingsCount();
        }
        return totalAwaitingDrivers;
    }
	
	/**
	 * Tells all regions to finish existing bookings already allocated, and stop accepting new bookings
	 */
	public void shutdown() {
		for (NuberRegion region : regions.values() ) {
			region.shutdown();
		}
		System.out.println("NuberDispatch has shut down all regions.");
	}

}
