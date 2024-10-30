package nuber.students;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * Booking represents the overall "job" for a passenger getting to their destination.
 * 
 * It begins with a passenger, and when the booking is commenced by the region 
 * responsible for it, an available driver is allocated from dispatch. If no driver is 
 * available, the booking must wait until one is. When the passenger arrives at the destination,
 * a BookingResult object is provided with the overall information for the booking.
 * 
 * The Booking must track how long it takes, from the instant it is created, to when the 
 * passenger arrives at their destination. This should be done using Date class' getTime().
 * 
 * Booking's should have a globally unique, sequential ID, allocated on their creation. 
 * This should be multi-thread friendly, allowing bookings to be created from different threads.
 * 
 * @author james
 *
 */
public class Booking {
	private static final AtomicInteger bookingCounter = new AtomicInteger(1);
    private final int jobID;
    private final Passenger passenger;
    private final NuberDispatch dispatch;
    private Driver driver;
    private long startTime;
    private boolean isCompleted = false;
    
	/**
	 * Creates a new booking for a given Nuber dispatch and passenger, noting that no
	 * driver is provided as it will depend on whether one is available when the region 
	 * can begin processing this booking.
	 * 
	 * @param dispatch
	 * @param passenger
	 */
	public Booking(NuberDispatch dispatch, Passenger passenger)
	{
        this.dispatch = dispatch;
        this.passenger = passenger;
        this.jobID = bookingCounter.getAndIncrement();
        this.startTime = 0;
	}
	
    private static synchronized int getNextjobID() {
        return bookingCounter++;
    }
    
    public synchronized void startBooking(Driver driver) {
        if (isCompleted) {
            System.err.println("Attempted to start a completed booking: " + this);
            return;
        }
        this.driver = driver;
        this.startTime = new Date().getTime();
        notify();
    }
    
	public Driver getDriver() {
	    return driver;
	}
	
	/**
	 * At some point, the Nuber Region responsible for the booking can start it (has free spot),
	 * and calls the Booking.call() function, which:
	 * 1.	Asks Dispatch for an available driver
	 * 2.	If no driver is currently available, the booking must wait until one is available. 
	 * 3.	Once it has a driver, it must call the Driver.pickUpPassenger() function, with the 
	 * 			thread pausing whilst as function is called.
	 * 4.	It must then call the Driver.driveToDestination() function, with the thread pausing 
	 * 			whilst as function is called.
	 * 5.	Once at the destination, the time is recorded, so we know the total trip duration. 
	 * 6.	The driver, now free, is added back into Dispatch’s list of available drivers. 
	 * 7.	The call() function the returns a BookingResult object, passing in the appropriate 
	 * 			information required in the BookingResult constructor.
	 *
	 * @return A BookingResult containing the final information about the booking 
	 */
	public synchronized BookingResult call() {
	    try {
	            // If no driver is available, wait until one is assigned

	                while (driver == null) {
	                    wait();
	                }


	        System.out.println(this + ": Starting, on way to passenger");
	        driver.pickUpPassenger(passenger);

	        System.out.println(this + ": Collected passenger, on way to destination");
	        driver.driveToDestination();
	        
	        System.out.println(this + ": At destination, driver is now free");
	        
	        long tripDuration = (new Date()).getTime() - startTime;
	        return new BookingResult(jobID, passenger, driver, tripDuration);

	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	        System.err.println("Booking interrupted: " + e.getMessage());
	        
	        return new BookingResult(jobID, passenger, null, -1);
	    } finally {
	        if (driver != null) {
	            dispatch.addDriver(driver);
	        }
	 }
}
	
	/***
	 * Should return the:
	 * - booking ID, 
	 * - followed by a colon, 
	 * - followed by the driver's name (if the driver is null, it should show the word "null")
	 * - followed by a colon, 
	 * - followed by the passenger's name (if the passenger is null, it should show the word "null")
	 * 
	 * @return The compiled string
	 */
	@Override
	public String toString()
	{
        return jobID + ":" + (driver == null ? "null" : driver.name) + ":" + 
                (passenger == null ? "null" : passenger.name);
	}

}
