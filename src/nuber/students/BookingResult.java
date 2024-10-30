package nuber.students;

public class BookingResult {

	public final int jobID;
	public final Passenger passenger;
	public final Driver driver;
	public final long tripDuration;
	
	public BookingResult(int jobID, Passenger passenger, Driver driver, long tripDuration)
	{
		this.jobID = jobID;
		this.passenger = passenger;
		this.driver = driver;
		this.tripDuration = tripDuration;
	}
	
	@Override
	public String toString()
	{
        return jobID + ":" + (driver == null ? "null" : driver.name) + ":" + 
                (passenger == null ? "null" : passenger.name);
	}

}
