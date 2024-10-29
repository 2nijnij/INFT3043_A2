package nuber.students;

import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	private final int maxSimultaneousJobs;
	private boolean isShuttingDown = false;
	
	private Queue<Booking> pendingBookings = new LinkedList<>();
	private int activeBookingsCount = 0;
	private ExecutorService executorService;

	
	/**
	 * Creates a new Nuber region
	 * 
	 * @param dispatch The central dispatch to use for obtaining drivers, and logging events
	 * @param regionName The regions name, unique for the dispatch instance
	 * @param maxSimultaneousJobs The maximum number of simultaneous bookings the region is allowed to process
	 */
	public NuberRegion(NuberDispatch dispatch, String regionName, int maxSimultaneousJobs)
	{
		this.dispatch = dispatch;
		this.regionName = regionName;
		this.maxSimultaneousJobs = maxSimultaneousJobs;
		this.executorService = Executors.newFixedThreadPool(maxSimultaneousJobs);
		System.out.println("Creating Nuber region for " + regionName);
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
	public synchronized Future<BookingResult> bookPassenger(Passenger passenger) {
		if (isShuttingDown) {
			System.out.println("Booking for passenger" + passenger.name + " rejected because region is shutting down." );
			return null;
		}
		
		Booking booking = new Booking(dispatch, passenger);
		pendingBookings.offer(booking);
		System.out.println(booking + ": Creating booking");
		
		processPendingBookings();
		
		FutureTask<BookingResult> bookingTask = new FutureTask<>(() -> booking.call());
		executorService.submit(bookingTask);
		return bookingTask;
}
	private synchronized void processPendingBookings() {
		while (activeBookingsCount < maxSimultaneousJobs && !pendingBookings.isEmpty()) {
			Booking booking = pendingBookings.poll();
			Driver driver = dispatch.getDriver();
			
			if (driver != null) {
				booking.startBooking(driver);
				activeBookingsCount++;
				System.out.println(booking + ": Starting booking, getting driver");
				
				executorService.submit(() -> {
					BookingResult result = booking.call();
					completeBooking(booking);
					
					if (result != null) {
						System.out.println(result);
					}

	            });
			} else {
				pendingBookings.offer(booking);
				break;
			}
		}
	}
			
	private synchronized void completeBooking(Booking booking) {
		activeBookingsCount--;
		dispatch.addDriver(booking.getDriver());
		System.out.println(booking + ": Driver is now free, booking complete");
		processPendingBookings();
	}
		
	
	/**
	 * Called by dispatch to tell the region to complete its existing bookings and stop accepting any new bookings
	 */
	public synchronized void shutdown()
	{
		isShuttingDown = true;
		System.out.println("Region " + regionName + " is shutting down.");

		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
	            System.err.println("Executor service did not terminate in the specified time.");
	            executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	public int getActiveBookingsCount() {
		return activeBookingsCount;
	}

	public int getPendingBookingsCount() {
		return pendingBookings.size();
	}
		
}
