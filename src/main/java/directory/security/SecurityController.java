package directory.security;

import java.util.Base64;
import com.github.jsonldjava.shaded.com.google.common.net.HttpHeaders;

import directory.Directory;
import directory.Utils;
import directory.exceptions.DirectoryAuthorizationException;
import directory.storage.CredentialsSQLFactory;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;

public class SecurityController {

	// -- Attribute
	private static final String BEARER_TOKEN = "Bearer";

	// -- Constructor
	private SecurityController() {
		super();
	}
	
	// -- Methods
	
	public static final Filter filterRequests = (Request request, Response response) -> {
		if(Directory.configuration.isEnableSecurity()) {
			Boolean authenticated = jwtFromHeader(request);
			if (!authenticated) {
				// Check basic authentication
				String credentials = getBasicAuthCredentials(request);
				String[] authorisation = credentials.split(":");
				Boolean exists = CredentialsSQLFactory.existsUser(authorisation[0], authorisation[1]);
				if (!exists)
					throw new DirectoryAuthorizationException();
			}
		}
	};
	
	private static final Boolean jwtFromHeader(Request request) {
		Boolean authenticated = false;
		try {
			String requestTokenHeader = request.headers(HttpHeaders.AUTHORIZATION);
			if (requestTokenHeader != null && requestTokenHeader.startsWith(BEARER_TOKEN)) {
				String jwt = requestTokenHeader.substring(7);
				String encodedUsername = JwtTokenUtil.getUsernameFromToken(jwt);
				Boolean validToken = CredentialsSQLFactory.existsUser(encodedUsername) && !JwtTokenUtil.isTokenExpired(jwt);
				if (!validToken)
					throw new DirectoryAuthorizationException();
			}
		} catch (Exception e) {
			throw new DirectoryAuthorizationException();
		}
		return authenticated;
	}
	
	public static final Route generateToken = (Request request, Response response) -> {		
		String credentials = getBasicAuthCredentials(request); // it is never null
		String payload = "";

		String[] authorisation = credentials.split(":");
		Boolean exists = CredentialsSQLFactory.existsUser(authorisation[0], authorisation[1]);
		if(!exists)
			throw new DirectoryAuthorizationException();
		String jwt = JwtTokenUtil.generateToken(authorisation[0]);
		payload = Utils.buildMessage("{\"token\" : \"",jwt,"\"}");
		response.status(200);
		return payload;
	};
	
	
	protected static String getBasicAuthCredentials(Request request) {
		String credentials = request.headers(HttpHeaders.AUTHORIZATION);
		if(credentials==null || !credentials.startsWith("Basic "))
			throw new DirectoryAuthorizationException();
		credentials = credentials.substring(6);
		byte[] decodedBytes = Base64.getDecoder().decode(credentials);
		return new String(decodedBytes);
	}

	

}
