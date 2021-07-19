package directory.users;

import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.validator.internal.util.classhierarchy.Filters;

import com.github.jsonldjava.shaded.com.google.common.net.HttpHeaders;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;

import directory.Utils;
import directory.exceptions.SearchJsonPathException;
import directory.search.JsonPathController;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;
import static spark.Spark.halt;
import things.ThingsDAO;

public class SecurityController {

	// -- Attribute
	private static final String BEARER_TOKEN = "Bearer";
	
	// -- Constructor
	private SecurityController() {
		super();
	}
	
	// -- Methods
	
	public static final Filter filterRequests = (Request request, Response response) -> {	
		String jwt = jwtFromHeader(request);
		if(jwt==null)
			halt(401, "{\"message\" : \"unauthorised\"}");
		//jwt = jwtFromCookie(request);
		//JwtTokenUtil.validateToken(jwt, originalUsername);
			
	};
	
	private static final String jwtFromHeader(Request request ) {
		String jwt = null;
		String requestTokenHeader = request.headers(HttpHeaders.AUTHORIZATION);
		if (requestTokenHeader!=null && requestTokenHeader.startsWith(BEARER_TOKEN)) {
			jwt = requestTokenHeader.substring(7);
		}
		return jwt;
	}
	
	private static final String jwtFromCookie(Request request ) {
		String jwt = null;
		String requestTokenHeader = request.headers(HttpHeaders.AUTHORIZATION);
		if (requestTokenHeader!=null && requestTokenHeader.startsWith(BEARER_TOKEN)) {
			jwt = requestTokenHeader.substring(7);
		}
		return jwt;
	}
	
	/*
	public static final Route login = (Request request, Response response) -> {		
		Map<String, Object> model = new HashMap<>();
		return Utils.render(model, "login.vm");	
	};*/
	
	private static final String KEY_USERNAME = "username";
	private static final String KEY_PASSWORD = "password";
	public static final Route login = (Request request, Response response) -> {		
		String credentials = basicAuth(request);
		String payload = "";
		if(credentials!=null) {
			String[] authorisation = credentials.split(":");
			Boolean exists = SQLFactory.existsUser(authorisation[0], authorisation[1]);
			if(exists) {
				System.out.println(authorisation[0]);
				System.out.println(authorisation[1]);
				String jwt = JwtTokenUtil.generateToken(authorisation[0]);
				payload = Utils.buildMessage("{\"token\" : \"",jwt,"\"}");
			}else {
				halt(401);
			}
		}else {
			// throw exception
			halt(401);
		}
		return payload;
	};
	
	private static String basicAuth(Request request) {
		String credentials = request.headers("Authorization");
		if(credentials!=null && credentials.startsWith("Basic ")) {
			credentials = credentials.substring(6);
			byte[] decodedBytes = Base64.getDecoder().decode(credentials);
			String decodedString = new String(decodedBytes);
			return decodedString;
		}
		return null;
	}
	

}
