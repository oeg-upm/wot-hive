package directory.exceptions;

import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class DirectoryAuthorizationException extends RuntimeException{

	private static final long serialVersionUID = -7384293668578068189L;

	public DirectoryAuthorizationException() {
		super();
	}
	
	public DirectoryAuthorizationException(String msg) {
		super(msg);
	}
	
	
	public static final ExceptionHandler handleException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(401);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		response.body("");
	};

}
