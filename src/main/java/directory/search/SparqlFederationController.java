package directory.search;

import java.util.List;
import java.util.UUID;

import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.resultset.ResultsFormat;

import com.github.jsonldjava.shaded.com.google.common.collect.Lists;

import directory.exceptions.SearchSparqlException;
import directory.triplestore.Sparql;
import spark.Request;
import spark.Response;
import spark.Route;

public class SparqlFederationController extends SparqlController {

	// -- Constructor
	protected SparqlFederationController() {
		// empty
	}

	// -- Methods
	public static final Route solveSparqlQueryFederated = (Request request, Response response) -> {
		List<String> urls = extractFederationEndpoints(request);
		String query = extractQuery(request);

		if (query == null)
			throw new SearchSparqlException(SearchSparqlException.EXCEPTION_CODE_1);
		if (!QueryFactory.create(query).isSelectType())
			throw new SearchSparqlException("Provided SPARQL query provided must be SELECT");

		String queryModified = rewriteQuery(query, urls);
		System.out.println("federated query:");
		System.out.println(queryModified);
		System.out.println("_____");
		ResultsFormat format = validateQueryAndMime(query, request.headers("Accept"));
		if (format == null)
			throw new SearchSparqlException("Provided mime type is not supported");

		return Sparql.query(queryModified, format);

	};

	private static List<String> extractFederationEndpoints(Request request) {
		String endpoints = request.queryParams("endpoints");
		
		try {
			if(endpoints == null) 
				throw new SearchSparqlException("Provide a list of endpoints using argument ?endpoints");
			if(endpoints.charAt(',') > 0)
				return Lists.newArrayList(endpoints.split(","));
		} catch (IndexOutOfBoundsException e) {
			return Lists.newArrayList(endpoints);
		}
		return null;

	}

	private static String rewriteQuery(String parsedQuery, List<String> endpoints) {
		int firstCut = parsedQuery.indexOf('{');
		int secondCut = parsedQuery.lastIndexOf('}');
		String serviceVar = ("?services-" + UUID.randomUUID().toString()).replaceAll("-", "");

		StringBuilder newQuery = new StringBuilder(parsedQuery.substring(0, firstCut + 1));

		newQuery.append("\nSERVICE ").append(serviceVar).append(" {").append(parsedQuery.substring(firstCut + 1, secondCut));
		newQuery.append("} VALUES ").append(serviceVar).append(" {");

		endpoints.parallelStream().filter(elem -> !elem.isBlank()).forEach(elem -> newQuery.append("<").append(elem).append("> "));
		newQuery.append(" }").append(parsedQuery.substring(secondCut));

		return newQuery.toString();
	}

}
