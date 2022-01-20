package directory.configuration;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import directory.Directory;
import directory.exceptions.ConfigurationException;

public class TriplestoreConfiguration extends AbstractConfiguration{

	// -- Attributes
	private URI updateEnpoint = null;
	private URI queryEnpoint = null;
	private String username = null;
	private String password = null;
	
	// -- Constructors
	
	public TriplestoreConfiguration() {
		super();
	}
	
	public TriplestoreConfiguration( String queryEnpoint, String updateEnpoint) throws URISyntaxException {
		super();
		this.updateEnpoint = new URI(updateEnpoint);
		this.queryEnpoint = new URI(queryEnpoint);

	}

	public TriplestoreConfiguration( URI queryEnpoint, URI updateEnpoint) {
		super();
		this.updateEnpoint = updateEnpoint;
		this.queryEnpoint = queryEnpoint;
	}

	// -- Getters & Setters

	public URI getUpdateEnpoint() {
		return updateEnpoint;
	}

	public void setUpdateEnpoint(URI updateEnpoint) {
		this.updateEnpoint = updateEnpoint;
	}

	public URI getQueryEnpoint() {
		return queryEnpoint;
	}

	public void setQueryEnpoint(URI queryEnpoint) {
		this.queryEnpoint = queryEnpoint;
	}
		
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	// Serialization methods
	
	public static TriplestoreConfiguration serialiseFromJson(String rawJson) {
		JsonObject body = null;
		try {
			body = (new Gson()).fromJson(rawJson, JsonObject.class);
		}catch(Exception e) {
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_3, e.toString());
		}
		if (body==null)
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_1, "A valid JSON configuration must be provided containing three mandatory keys: \"queryEnpoint\" and  \"updateEnpoint\" pointing to the the triplestore URL endpoint for querying and updating data respectivelly and \"queryUsingGET\" with a boolean value for communicating using the GET method (if true, recommended) or POST (if set to false). For instance '{\"updateEnpoint\":\"http://localhost:3030/sparql\",\"queryEnpoint\":\"http://localhost:3030/sparql\",\"queryUsingGET\":true}'");	
		
		validatePayload( body, "updateEnpoint", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"updateEnpoint\" with the triplestore endpoint for updating data");
		validatePayload( body, "queryEnpoint", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"queryEnpoint\" with the triplestore endpoint for querying data");
		if((!body.has("username") && body.has("password")) ||(body.has("username") && !body.has("password")))
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_2, "Provided JSON must have both optional keywords \"username\" and \"passwords\" in order to connect to a remote triple store");
		if(!body.has("username"))
			Directory.LOGGER.info("No 'username' has been provided for the remote triple store");
		if(!body.has("password"))
			Directory.LOGGER.info("No 'password' has been provided for the remote triple store");
		
		return (new GsonBuilder().serializeNulls().create()).fromJson(body, TriplestoreConfiguration.class);
	}
	

	
	
}
