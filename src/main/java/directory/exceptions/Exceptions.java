package directory.exceptions;


import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class Exceptions {

	// -- Attributes
	public static final String ERROR_MIME = "application/problem+json";
	// -- Constructor 
	
	private Exceptions() {
		super();
	}
	
	// -- Methods
	
	public static final ExceptionHandler handleThingRegistrationException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, ERROR_MIME);
		response.body(Utils.createErrorMessage("WOT-DIR-R", "Invalid serialization or TD", exception.toString()));
	};

	public static final ExceptionHandler handleThingParsingException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, ERROR_MIME);
		response.body(Utils.createErrorMessage("WOT-DIR-P", "Invalid serialization or TD", exception.toString()));
	};
	
	public static final ExceptionHandler handleRemoteException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, ERROR_MIME);
		response.body(Utils.createErrorMessage("WOT-DIR-P", "Remote triple store is not responding correctly.", exception.toString()));
	};
	

	
	public static final ExceptionHandler handleException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, ERROR_MIME);
		response.body(Utils.createErrorMessage("WOT-DIR-R", "Unknown exception", exception.toString()));
	};
	

	
	
	
}
