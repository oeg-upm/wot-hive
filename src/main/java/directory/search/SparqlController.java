package directory.search;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.resultset.ResultsFormat;

import directory.Utils;
import directory.exceptions.SearchSparqlException;
import directory.triplestore.Sparql;
import spark.Request;
import spark.Response;
import spark.Route;

public class SparqlController {
	
	// -- Attributes 
	/** @see: <a href="https://www.w3.org/TR/sparql11-results-json/"> SPARQL documentation </a> **/
	private static final String MIME_SPARQL_JSON = "application/sparql-results+json";
	/** @see: <a href="https://www.w3.org/TR/rdf-sparql-XMLres/"> SPARQL documentation </a> **/
	private static final String MIME_SPARQL_XML = "application/sparql-results+xml";
	/** @see: <a href="https://www.w3.org/TR/2013/REC-sparql11-results-csv-tsv-20130321/"> SPARQL documentation </a>**/
	private static final String MIME_SPARQL_CSV = "text/csv";
	private static final String MIME_SPARQL_TSV = "text/tab-separated-values";
	private static final String MIME_SPARQL_DEFAULT = "*/*";
	
	// -- Constructor
	protected SparqlController() {
		super();
	}
	
	// -- Methods
	public static final Route solveSparqlQuery = (Request request, Response response) -> {
		String query = extractQuery(request);
		String accept = request.headers("Accept");
		if(query==null)
			throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_1);
		
		ResultsFormat format = validateQueryAndMime(query, accept) ;
		if(format ==null)
			throw new SearchSparqlException("Provided mime type is not supported");
		
		String type = extractType(format);
		response.type(type);
		return Sparql.query(query, format);
    };
    
	
    
    protected static ResultsFormat validateQueryAndMime(String query, String mimeType) {
	    Query parsedQuery = QueryFactory.create(query);
		if(parsedQuery.isAskType() || parsedQuery.isSelectType()) {
			if(mimeType==null) 
				return ResultsFormat.FMT_RS_JSON;
			if(mimeType.equals(MIME_SPARQL_TSV)) {
				return ResultsFormat.FMT_RS_TSV;	
			}else if( mimeType.equals(MIME_SPARQL_CSV)) {
				return ResultsFormat.FMT_RS_CSV;
			}else if( mimeType.equals(MIME_SPARQL_XML)) {
				return ResultsFormat.FMT_RS_XML;
			}else {
				return ResultsFormat.FMT_RS_JSON; // || mimeType.equals(MIME_SPARQL_DEFAULT)
			}
    		//throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_3, Utils.buildMessage("Supported SPARQL mime types are 'application/sparql-results+json' (default if no mime is provided), 'application/sparql-results+xml', 'text/csv', 'text/tab-separated-values' for SELECT and ASK. Provided instead ", mimeType));
		}else if(parsedQuery.isConstructType() || parsedQuery.isDescribeType()) {
			//if(mimeType==null || mimeType.equals(MIME_SPARQL_DEFAULT)) 
			return ResultsFormat.FMT_RDF_TURTLE;
			//if(mimeType.equals(Utils.MIME_THING) || !Utils.WOT_TD_MYMES.containsKey(mimeType))
			//	throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_3, Utils.buildMessage("Supported SPARQL mime types are for CONSTRUCT and DESCRIBE queries are ", Utils.WOT_TD_MYMES.toString(), ". Provided instead ", mimeType));
		}else {
			throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_3, "Supported SPARQL queries are SELECT, ASK, DESCRIBE, and CONSTRUCT");
		}
 
		
    }
    
   
    
    
  
    
    private static String extractType(ResultsFormat format) {
    	if(format.equals(ResultsFormat.FMT_RS_JSON))
    		return MIME_SPARQL_JSON;
    	if(format.equals(ResultsFormat.FMT_RS_CSV))
    		return MIME_SPARQL_CSV;
    	if(format.equals(ResultsFormat.FMT_RS_TSV))
    		return MIME_SPARQL_TSV;
    	if(format.equals(ResultsFormat.FMT_RS_XML))
    		return MIME_SPARQL_XML;
    	if(format.equals(ResultsFormat.FMT_RDF_TURTLE))
    		return "text/turtle";
    	return MIME_SPARQL_JSON;
	}






	protected static String extractQuery(Request request) {
	 	String query = null;
	 	if(Utils.METHOD_GET.equals(request.requestMethod())) {
			query = request.queryParams("query");
		}else if (Utils.METHOD_POST.equals(request.requestMethod())) {
			query = request.body();
			if(query==null || query.isEmpty())
				query = request.queryParams("query");
			if(query.contains("infer=") && query.contains("queryLn=SPARQL")) {
				// graphDb federation
				String[] fragments = query.split("&");
				for(String fragment:fragments) {
					if(fragment.startsWith("query=")) {
						try {
							query = URLDecoder.decode(fragment.substring(fragment.indexOf("=")+1), "UTF-8" );
						} catch (UnsupportedEncodingException e) {
							throw new SearchSparqlException(e.toString());
							
						}
						break;
					}
				}
			}
		}
	 	return query;
 }
}
