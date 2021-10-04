package directory.exceptions;

public class ThingParsingException extends RuntimeException{

	private static final long serialVersionUID = -8136363515432821890L;

	public ThingParsingException() {
		super();
	}
	
	public ThingParsingException(String msg) {
		super(msg);
	}
	
	
}
