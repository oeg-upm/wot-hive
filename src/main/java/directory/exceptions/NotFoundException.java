package directory.exceptions;

import directory.Directory;
import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class NotFoundException extends RuntimeException{

	private static final long serialVersionUID = 2169467740042347789L;

	public NotFoundException() {
		super();
	}
	
	public NotFoundException(String msg) {
		super(msg);
	}
	
	@SuppressWarnings("rawtypes")
	public static final ExceptionHandler handleNotFoundExceptionException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(404);
		response.header("charset", "utf-8");
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		response.body(Utils.createErrorMessage("hive:error:things", "Error happened manipulating Things", exception.toString()));
		String logStr = Utils.buildMessage(request.requestMethod(), " (",String.valueOf(response.status()),") ", request.pathInfo()+" \nmessage:",exception.toString());
		Directory.LOGGER.info(logStr);
	};

}
