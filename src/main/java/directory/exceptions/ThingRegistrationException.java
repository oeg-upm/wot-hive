package directory.exceptions;

public class ThingRegistrationException extends RuntimeException{

	private static final long serialVersionUID = 2169467740042347789L;

	public ThingRegistrationException() {
		super();
	}
	
	public ThingRegistrationException(String msg) {
		super(msg);
	}
}
