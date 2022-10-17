package directory.exceptions;

import directory.Directory;
import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class ConfigurationException extends RuntimeException {

	// -- Attributes
	private static final long serialVersionUID = -5638010557314138343L;
	private String code;
	
	// --Constructors
	
	public ConfigurationException() {
		super();
	}
	
	public ConfigurationException(String code) {
		super();
		this.code = code;
	}
	
	public ConfigurationException(String code, String msg) {
		super(msg);
		this.code = code;
	}
	
	// -- Getters
	
	public String getCode() {
		return this.code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	// -- Exception handler methods
	
	public static final String EXCEPTION_CODE_1 = "configuration-001"; // no configuration provided
	public static final String EXCEPTION_CODE_2 = "configuration-002"; // missing mandatory key
	public static final String EXCEPTION_CODE_3 = "configuration-003"; // unknown error

	

	
	@SuppressWarnings("rawtypes")
	public static final ExceptionHandler handleConfigurationException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header("charset", "utf-8");
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		ConfigurationException specificException = (ConfigurationException) exception;
		String logStr = Utils.buildMessage(request.requestMethod(), " (",String.valueOf(response.status()),") ", request.pathInfo()+" \nmessage:",exception.toString());
		Directory.LOGGER.info(logStr);
		response.body(Utils.createErrorMessage(specificException.code, "JSON configuration not provided, is blank, or contains syntax errors", exception.toString()));
	};


}
