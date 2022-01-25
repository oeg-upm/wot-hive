package directory.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import directory.Utils;
import directory.exceptions.SearchJsonPathException;
import directory.things.ThingsService;
import net.minidev.json.JSONArray;
import spark.Request;
import spark.Response;
import spark.Route;


public class JsonPathController{

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
		List<String> thingsIds = ThingsService.retrieveThingsIds(null, null);
		return thingsIds.parallelStream().map(ThingsService::retrieveThing).flatMap(thing -> mapJsonPath(thing, path).stream()).collect(Collectors.toList());
	};
    
	private static JsonPath checkJsonPath(String jsonPath) {
		JsonPath path = JsonPath.compile(jsonPath);
		if(!jsonPath.startsWith("$"))
			throw new SearchJsonPathException(SearchJsonPathException.EXCEPTION_CODE_2);
		return path;
	}
    

	private static List<String> mapJsonPath(JsonObject thing, JsonPath path) {
    		List<String> result = new ArrayList<>();
    		try {
    			Object pathResult = JsonPath.parse(thing.toString()).read(path);
			if(!path.isDefinite()) {
				((JSONArray) pathResult).forEach(elem -> result.add( instantiateJson(elem) ));
			}else {
				String tmpElement = instantiateJson(pathResult);
				if(tmpElement!=null)
					result.add(tmpElement);
			}
    		} catch (PathNotFoundException e) {
			throw new SearchJsonPathException(SearchJsonPathException.EXCEPTION_CODE_3);
		}

    		return result;
    }
    
    
    
	private static String instantiateJson(Object pathResult) {
    		String result = null;
		try {
			// Process pathResult as Json
			result = new ObjectMapper().writeValueAsString(pathResult);
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
