package directory.exceptions;

import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class SearchXPathException  extends RuntimeException {

	private static final long serialVersionUID = 588858299068660760L; 
	private String code;
	
	public SearchXPathException() {
		super();
	}
	
	public SearchXPathException(String code) {
		super();
		this.code = code;
	}
	
	public SearchXPathException(String code, String msg) {
		super(msg);
		this.code = code;
	}
	
	public static final String EXCEPTION_CODE_1 = "searchXPath-001"; // no query provided
	public static final String EXCEPTION_CODE_2 = "searchXPath-002"; // xpath path invalid syntax
	public static final String EXCEPTION_CODE_3 = "searchXPath-003"; // error applying xpath over json
	
	public static final ExceptionHandler handleSearchXPathException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		SearchXPathException specificException = (SearchXPathException) exception;
		response.body(Utils.createErrorMessage(specificException.code, "XPath expression not provided or contains syntax errors", exception.toString()));
	};

}
