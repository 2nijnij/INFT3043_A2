package nuber.students;

import java.util.Random;

public class Driver extends Person {

	private Passenger currentPassenger;
	
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
	public void pickUpPassenger(Passenger newPassenger) throws InterruptedException {
		this.currentPassenger = newPassenger;
		
		Random random = new Random();
		// random delay between 0-maxDelay milliseconds.
		int pickUpDelay = random.nextInt(maxSleep + 1);
		System.out.println("Driver " + name + " is picking up passenger " + newPassenger.name + " (delay: " + pickUpDelay + "ms)");
	    Thread.sleep(pickUpDelay);
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
	        System.out.println("Driver " + name + " is currently driving passenger " + currentPassenger.name + " to destination (travel time: " + travelTime + "ms)");
	        Thread.sleep(travelTime);
	        System.out.println("Driver " + name + " has reached the destination with a passenger " + currentPassenger.name);
	        
	        currentPassenger = null;
		} else {
			System.out.println("No passenger is assigned to driver" + name + "to drive passenger's destination.");
		}

	}
	
}
