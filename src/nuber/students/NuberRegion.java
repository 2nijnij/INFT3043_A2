package nuber.students;

import java.util.concurrent.Future;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;


/**
 * A single Nuber region that operates independently of other regions, other than getting 
 * drivers from bookings from the central dispatch.
 * 
 * A region has a maxSimultaneousJobs setting that defines the maximum number of bookings 
 * that can be active with a driver at any time. For passengers booked that exceed that 
 * active count, the booking is accepted, but must wait until a position is available, and 
 * a driver is available.
 * 
 * Bookings do NOT have to be completed in FIFO order.
 * 
 * @author james
 *
 */
public class NuberRegion {
    private final NuberDispatch dispatch;
    private final String regionName;
    private final Semaphore activeJobsSemaphore;
    private final ExecutorService bookingExecutor = Executors.newCachedThreadPool();
    private volatile boolean shutdown = false;
    private final Set<Integer> bookingsCreated = ConcurrentHashMap.newKeySet();

	/**
	 * Creates a new Nuber region
	 * 
	 * @param dispatch The central dispatch to use for obtaining drivers, and logging events
	 * @param regionName The regions name, unique for the dispatch instance
	 * @param maxSimultaneousJobs The maximum number of simultaneous bookings the region is allowed to process
	 */
    public NuberRegion(NuberDispatch dispatch, String regionName, int maxSimultaneousJobs) {
        this.dispatch = dispatch;
        this.regionName = regionName;
        this.activeJobsSemaphore = new Semaphore(maxSimultaneousJobs);
    }
	
	/**
	 * Creates a booking for given passenger, and adds the booking to the 
	 * collection of jobs to process. Once the region has a position available, and a driver is available, 
	 * the booking should commence automatically. 
	 * 
	 * If the region has been told to shutdown, this function should return null, and log a message to the 
	 * console that the booking was rejected.
	 * 
	 * @param waitingPassenger
	 * @return a Future that will provide the final BookingResult object from the completed booking
	 */
    public Future<BookingResult> bookPassenger(Passenger waitingPassenger) {
        if (shutdown) {
            dispatch.logEvent(new Booking(dispatch, waitingPassenger), "Booking rejected: Region is shutting down.");
            return null;
        }

        Booking booking = new Booking(dispatch, waitingPassenger);
        int passengerId = System.identityHashCode(waitingPassenger);

        synchronized (bookingsCreated) {
            if (bookingsCreated.add(passengerId)) {
                dispatch.logEvent(booking, "Creating booking");
                dispatch.incrementBookingsAwaitingDriver();
            }
        }

        return bookingExecutor.submit(() -> {
            try {
                activeJobsSemaphore.acquire();
                Driver assignedDriver = null;
                for (int retryCount = 3; retryCount > 0 && assignedDriver == null; retryCount--) {
                    assignedDriver = dispatch.getDriver(5000);
                }
                if (assignedDriver == null) {
                    dispatch.logEvent(booking, "Booking failed - no driver available after retries.");
                    dispatch.decrementBookingsAwaitingDriver();
                    return null;
                }

                booking.assignDriver(assignedDriver);
                dispatch.decrementBookingsAwaitingDriver();
                return booking.call();

            } finally {
                activeJobsSemaphore.release();
            }
        });
    }

	/**
	 * Called by dispatch to tell the region to complete its existing bookings and stop accepting any new bookings
	 */
    public void shutdown() {
        shutdown = true;
        bookingExecutor.shutdown();

        try {
            if (!bookingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                bookingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            bookingExecutor.shutdownNow();
        }
        dispatch.logEvent(null, "Region " + regionName + " shutdown complete.");
    }
    
    public void awaitTermination() throws InterruptedException {
        bookingExecutor.shutdown();
        bookingExecutor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
