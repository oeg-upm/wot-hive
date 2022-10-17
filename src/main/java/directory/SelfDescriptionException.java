package directory;

import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class SelfDescriptionException extends RuntimeException {

	private static final long serialVersionUID = -3669926764233087817L;
	
	public SelfDescriptionException(String msg) {
		super(msg);
	}
	
	
	
	@SuppressWarnings("rawtypes")
	public static final ExceptionHandler handleSelfDescriptionException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(404);
		response.header("charset", "utf-8");
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		response.body(Utils.createErrorMessage("WOT-DIR-Introduction", "Self description exception", exception.toString()));
	};
	
	

}
