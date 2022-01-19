package directory.exceptions;

import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class RemoteSparqlEndpointException extends RuntimeException{

	private static final long serialVersionUID = -7384293668578068189L;

	public RemoteSparqlEndpointException() {
		super();
	}
	
	public RemoteSparqlEndpointException(String msg) {
		super(msg);
	}
	
	

	@SuppressWarnings("rawtypes")
	public static final ExceptionHandler handleRemoteException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		response.body(Utils.createErrorMessage("WOT-DIR-P", "Internal problem communnicating with remote SPARQL endpoint", exception.toString()));
	};

}
