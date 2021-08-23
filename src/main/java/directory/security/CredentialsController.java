package directory.security;

import com.google.gson.JsonObject;

import directory.Directory;
import directory.exceptions.DirectoryAutenticationException;
import directory.storage.CredentialsSQLFactory;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;
import wot.jtd.JTD;
import static spark.Spark.halt;

public class CredentialsController {

	// -- Attributes
	private static final String KEY_USERNAME = "username";
	private static final String KEY_SECRET = "password";
	// -- Constructor
	private CredentialsController() {
		super();
	}

	// -- Methods
	
	public static final Filter filterRequests = (Request request, Response response) -> {
		if(!Directory.configuration.isEnableSecurity())
			halt(404);
	};
	
	public static final Route retrieveCredentials = (Request request, Response response) -> {		
		String[] stored = CredentialsSQLFactory.getStoredCredentials(false);
		JsonObject credentials = new JsonObject();
		credentials.addProperty(KEY_USERNAME, stored[0]);
		credentials.addProperty("created", stored[2]);
		response.status(200);
		return credentials;
	};
	
	public static final Route updateCredentials = (Request request, Response response) -> {		
		try{
			JsonObject credentials = JTD.parseJson(request.body());
			if(!credentials.has(KEY_USERNAME) || !credentials.has(KEY_SECRET)) {
				throw new DirectoryAutenticationException(DirectoryAutenticationException.EXCEPTION_CODE_1,"Provided JSON lacks of mandatory keys 'username' or 'password'");
			}
			String username = credentials.get(KEY_USERNAME).getAsString();
			String password = credentials.get(KEY_SECRET).getAsString();
			if(username.isBlank())
				throw new DirectoryAutenticationException(DirectoryAutenticationException.EXCEPTION_CODE_2,"Provided username can not be blank");
			if(password.isBlank())
				throw new DirectoryAutenticationException(DirectoryAutenticationException.EXCEPTION_CODE_2,"Provided password can not be blank");
			CredentialsSQLFactory.updateCredentials(username, password);
			response.status(201);
		} catch(Exception e) {
			throw new DirectoryAutenticationException(DirectoryAutenticationException.EXCEPTION_CODE_1, "Provided JSON lacks of mandatory keys 'username' or 'password'");
		}
		return "";
	};
	
	
	
}
