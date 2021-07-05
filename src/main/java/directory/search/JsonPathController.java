package directory.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import directory.Utils;
import directory.exceptions.SearchJsonPathException;
import directory.td.ThingsDAO;
import net.minidev.json.JSONArray;
import spark.Request;
import spark.Response;
import spark.Route;
import wot.jtd.model.Thing;

public class JsonPathController {

	private JsonPathController() {
		super();
	}
	
	public static final Route solveJsonPath = (Request request, Response response) -> {		
		String query = request.queryParams("query");
		if(query==null)
			throw new SearchJsonPathException(SearchJsonPathException.EXCEPTION_CODE_1);
		JsonPath path = checkJsonPath(query);
		
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_JSON);
		response.status(200);
		
		return ThingsDAO.readAll().parallelStream().map(thing -> mapJsonPath(thing, path)).filter(JsonPathController::validList).flatMap(Collection::stream).collect(Collectors.toList());	
	};
    
	private static JsonPath checkJsonPath(String jsonPath) {
		JsonPath path = null;
		try {
			path = JsonPath.compile(jsonPath);
			if(!jsonPath.startsWith("$"))
				throw new SearchJsonPathException(SearchJsonPathException.EXCEPTION_CODE_2);
		}catch(Exception e ) {
			throw new SearchJsonPathException(SearchJsonPathException.EXCEPTION_CODE_2);
		}
		return path;
	}
    

	private static List<String> mapJsonPath(Thing thing, JsonPath path) {
    		List<String> result = new ArrayList<>();
    		try {
    			Object pathResult = JsonPath.parse(thing.toJson().toString()).read(path);

			if(!path.isDefinite()) {
				((JSONArray) pathResult).forEach(elem -> result.add( instantiateJson(elem) ));
			}else {
				String tmpElement = instantiateJson(pathResult);
				if(tmpElement!=null)
					result.add(tmpElement);
			}
    		} catch (JsonProcessingException e) {
			throw new SearchJsonPathException(SearchJsonPathException.EXCEPTION_CODE_3);
		} catch (PathNotFoundException e) {
			// Nothing to do, this means there was no response
		}

    		return result;
    }
    
    
    
    @SuppressWarnings("unchecked")
	private static String instantiateJson(Object pathResult) {
    		String result = null;
		try {
			// Process pathResult as Json
			result = new ObjectMapper().writeValueAsString((Map<String,Object>) pathResult);
		}catch(Exception e) { 
			// Process pathResult as String
			result = Utils.buildMessage("\"",pathResult.toString(),"\"");
		}
		return result;
	}

	private static Boolean validList(List<String> list) {
    		return list!=null && !list.isEmpty();
    }
    
}
