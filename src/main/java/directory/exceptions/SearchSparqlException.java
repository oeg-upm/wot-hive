package directory.exceptions;

import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class SearchSparqlException  extends RuntimeException {

	private static final long serialVersionUID = 588858299068660760L; 
	private String code;
	
	public SearchSparqlException() {
		super();
	}
	
	public SearchSparqlException(String code) {
		super();
		this.code = code;
	}
	
	public SearchSparqlException(String code, String msg) {
		super(msg);
		this.code = code;
	}
	
	public static final String EXCEPTION_CODE_1 = "searchSparql-001"; // no query provided
	public static final String EXCEPTION_CODE_2 = "searchSparql-002"; // Sparql path invalid syntax
	public static final String EXCEPTION_CODE_3 = "searchSparql-003"; // error applying Sparql
	
	@SuppressWarnings("rawtypes")
	public static final ExceptionHandler handleSearchSparqlException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		SearchSparqlException specificException = (SearchSparqlException) exception;
		response.body(Utils.createErrorMessage(specificException.code, "Sparql expression not provided or contains syntax errors", exception.toString()));
	};

}
