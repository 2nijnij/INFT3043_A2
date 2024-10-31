package nuber.students;

import java.util.Random;

public class Driver extends Person {
	
    private Passenger currentPassenger;
    private final Random random = new Random();

	public Driver(String driverName, int maxSleep)
	{
        super(driverName, maxSleep);
	}
	
	/**
	 * Stores the provided passenger as the driver's current passenger and then
	 * sleeps the thread for between 0-maxDelay milliseconds.
	 * 
	 * @param newPassenger Passenger to collect
	 * @throws InterruptedException
	 */
	public void pickUpPassenger(Passenger newPassenger)throws InterruptedException {
        this.currentPassenger = newPassenger;
        int sleepTime = random.nextInt(maxSleep + 1);
        Thread.sleep(sleepTime);
        System.out.println("Driver " + name + " picking up passenger " + newPassenger.name + " with delay " + sleepTime + " ms");
        Thread.sleep(sleepTime);
    }

	/**
	 * Sleeps the thread for the amount of time returned by the current 
	 * passenger's getTravelTime() function
	 * 
	 * @throws InterruptedException
	 */
	public void driveToDestination() throws InterruptedException {
        if (currentPassenger != null) {
            int travelTime = currentPassenger.getTravelTime();
            System.out.println("Driver " + name + " driving passenger " + currentPassenger.name + " to destination with travel time " + travelTime + " ms");
            Thread.sleep(travelTime);
        }
	}
	
}
