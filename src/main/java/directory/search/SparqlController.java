package directory.search;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;

import directory.SPARQLEndpoint;
import directory.Utils;
import directory.exceptions.SearchSparqlException;
import spark.Request;
import spark.Response;
import spark.Route;

public class SparqlController {
	/** @see: <a href="https://www.w3.org/TR/sparql11-results-json/"> SPARQL documentation </a> **/
	private static final String MIME_SPARQL_JSON = "application/sparql-results+json";
	/** @see: <a href="https://www.w3.org/TR/rdf-sparql-XMLres/"> SPARQL documentation </a> **/
	private static final String MIME_SPARQL_XML = "application/sparql-results+xml";
	/** @see: <a href="https://www.w3.org/TR/2013/REC-sparql11-results-csv-tsv-20130321/"> SPARQL documentation </a>**/
	private static final String MIME_SPARQL_CSV = "text/csv";
	private static final String MIME_SPARQL_TSV = "text/tab-separated-values";
	
	private static final String MIME_SPARQL_DEFAULT = "*/*";
	private SparqlController() {
		super();
	}
	
	
	public static final Route solveSparqlQuery = (Request request, Response response) -> {
		String query = extractQuery(request);
		if(query==null)
			throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_1);
		
		String validMime = validateQueryAndMime(query, request.headers("Accept")) ;
	
		return SPARQLEndpoint.sendQueryStream(query, validMime);
    };
    
    private static String validateQueryAndMime(String query, String mimeType) {
	    	Query parsedQuery = QueryFactory.create(query);
		if(parsedQuery.isAskType() || parsedQuery.isSelectType()) {
			if(mimeType==null || mimeType.equals(MIME_SPARQL_DEFAULT)) 
				mimeType = MIME_SPARQL_JSON;
			if(!mimeType.equals(MIME_SPARQL_TSV) && !mimeType.equals(MIME_SPARQL_CSV) && !mimeType.equals(MIME_SPARQL_JSON) && !mimeType.equals(MIME_SPARQL_XML))
    				throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_3, Utils.buildMessage("Supported SPARQL mime types are 'application/sparql-results+json' (default if no mime is provided), 'application/sparql-results+xml', 'text/csv', 'text/tab-separated-values' for SELECT and ASK. Provided instead ", mimeType));
		}else if(parsedQuery.isConstructType() || parsedQuery.isDescribeType()) {
			if(mimeType==null || mimeType.equals(MIME_SPARQL_DEFAULT)) 
				mimeType = Utils.MIME_TURTLE;
			if(mimeType.equals(Utils.MIME_THING) || !Utils.WOT_TD_MYMES.containsKey(mimeType))
				throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_3, Utils.buildMessage("Supported SPARQL mime types are for CONSTRUCT and DESCRIBE queries are ", Utils.WOT_TD_MYMES.toString(), ". Provided instead ", mimeType));
		}else {
			throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_3, "Supported SPARQL queries are SELECT, ASK, DESCRIBE, and CONSTRUCT");
		}
 
		return mimeType;
    }
    
    private static String extractQuery(Request request) {
	 	String query = null;
	 	if(Utils.METHOD_GET.equals(request.requestMethod())) {
			query = request.queryParams("query");
		}else if (Utils.METHOD_POST.equals(request.requestMethod())) {
			query = request.body();
			if(query==null || query.isEmpty())
				query = request.queryParams("query");
		}
	 	return query;
 }
}
