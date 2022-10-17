package directory.exceptions;

import directory.Directory;
import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class SearchJsonPathException  extends RuntimeException {

	private static final long serialVersionUID = 588858299068660760L; 
	private String code;
	
	public SearchJsonPathException() {
		super();
	}
	
	public SearchJsonPathException(String code) {
		super();
		this.code = code;
	}
	
	public SearchJsonPathException(String code, String msg) {
		super(msg);
		this.code = code;
	}
	
	public static final String EXCEPTION_CODE_1 = "searchJsonPath-001"; // no query provided
	public static final String EXCEPTION_CODE_2 = "searchJsonPath-002"; // json path invalid syntax
	public static final String EXCEPTION_CODE_3 = "searchJsonPath-003"; // error applying json path over json
	

	@SuppressWarnings("rawtypes")
	public static final ExceptionHandler handleSearchJsonPathException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header("charset", "utf-8");
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		SearchJsonPathException specificException = (SearchJsonPathException) exception;
		response.body(Utils.createErrorMessage(specificException.code, "JSONPath expression not provided or contains syntax errors", exception.toString()));
		String logStr = Utils.buildMessage(request.requestMethod(), " (",String.valueOf(response.status()),") ", request.pathInfo()+" \nmessage:",exception.toString());
		Directory.LOGGER.info(logStr);
	};

}
