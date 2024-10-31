package nuber.students;

public class BookingResult {

	public int bookingID;
	public Passenger passenger;
	public Driver driver;
	public long tripDuration;
	
	public BookingResult(int bookingID, Passenger passenger, Driver driver, long tripDuration)
	{
		this.bookingID = bookingID;
		this.passenger = passenger;
		this.driver = driver;
		this.tripDuration = tripDuration;
	}
	
    @Override
    public String toString() {
        return "BookingResult{" +
                "bookingID=" + bookingID +
                ", passenger=" + (passenger != null ? passenger.name : "null") +
                ", driver=" + (driver != null ? driver.name : "null") +
                ", tripDuration=" + tripDuration +
                '}';
    }
}
