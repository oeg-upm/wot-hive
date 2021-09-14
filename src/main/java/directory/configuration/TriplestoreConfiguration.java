package directory.configuration;

import java.net.URI;
import java.net.URISyntaxException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import directory.exceptions.ConfigurationException;

public class TriplestoreConfiguration extends AbstractConfiguration{

	// -- Attributes
	private URI updateEnpoint = null;
	private URI queryEnpoint = null;
	private Boolean queryUsingGET = true;
	
	// -- Constructors
	
	public TriplestoreConfiguration() {
		super();
	}
	
	public TriplestoreConfiguration( String queryEnpoint, String updateEnpoint, Boolean queryUsingGET) throws URISyntaxException {
		super();
		this.updateEnpoint = new URI(updateEnpoint);
		this.queryEnpoint = new URI(queryEnpoint);
		this.queryUsingGET = queryUsingGET;
	}

	public TriplestoreConfiguration( URI queryEnpoint, URI updateEnpoint, Boolean queryUsingGET) {
		super();
		this.updateEnpoint = updateEnpoint;
		this.queryEnpoint = queryEnpoint;
		this.queryUsingGET = queryUsingGET;
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

	public Boolean getQueryUsingGET() {
		return queryUsingGET;
	}

	public void setQueryUsingGET(Boolean queryUsingGET) {
		this.queryUsingGET = queryUsingGET;
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
		validatePayload( body, "queryUsingGET", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"queryUsingGET\" with a boolean value indicating if the communication with the triplestore shuld be done using the GET method. If false the directory will use POST.");
		
		return (new Gson()).fromJson(body, TriplestoreConfiguration.class);
	}
	

	
	
}
