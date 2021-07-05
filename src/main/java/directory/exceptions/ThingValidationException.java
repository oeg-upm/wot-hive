package directory.exceptions;

import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class ThingValidationException  extends RuntimeException {

	private static final long serialVersionUID = 588858299068660760L; 
	private String code;
	
	public ThingValidationException() {
		super();
	}
	
	public ThingValidationException(String code) {
		super();
		this.code = code;
	}
	
	public ThingValidationException(String code, String msg) {
		super(msg);
		this.code = code;
	}
	
	public static final String EXCEPTION_CODE_1 = "thingValidation-001"; 
	public static final String EXCEPTION_CODE_2 = "searchSparql-002"; 
	public static final String EXCEPTION_CODE_3 = "searchSparql-003"; 
	
	public static final ExceptionHandler handleSearchXPathException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		ThingValidationException specificException = (ThingValidationException) exception;
		response.body(Utils.createErrorMessage(specificException.code, "Sparql expression not provided or contains syntax errors", exception.toString()));
	};

}
