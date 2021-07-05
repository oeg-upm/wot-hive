package directory.exceptions;

import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class ThingNotFoundException extends RuntimeException{

	private static final long serialVersionUID = 2169467740042347789L;

	public ThingNotFoundException() {
		super();
	}
	
	public ThingNotFoundException(String msg) {
		super(msg);
	}
	
	public static final ExceptionHandler handleThingNotFoundException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(404);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		response.body(Utils.createErrorMessage("WOT-DIR-P", "TD with the given id not found", exception.toString()));
	};
}
