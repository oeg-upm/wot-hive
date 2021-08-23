package directory.exceptions;

import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class DirectoryAutenticationException extends RuntimeException{

	private static final long serialVersionUID = -7384293668578068189L;
	private String code;
	
	public DirectoryAutenticationException(String code) {
		super();
		this.code = code;
	}
	
	public DirectoryAutenticationException(String code, String msg) {
		super(msg);
		this.code = code;
	}
	
	public String getCode() {
		return code;
	}
	
	public static final String EXCEPTION_CODE_1 = "credentials-001"; // error in provided Json
	public static final String EXCEPTION_CODE_2 = "credentials-002"; // error in provided username/password is blank

	public static final ExceptionHandler handleException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(401);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		DirectoryAutenticationException specificException = (DirectoryAutenticationException) exception;
		response.body(Utils.createErrorMessage(specificException.getCode(), "Provided JSON credentials have problems", null));
	};

}
