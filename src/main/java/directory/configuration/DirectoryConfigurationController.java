package directory.configuration;


import org.apache.http.HttpHeaders;

import directory.Directory;
import directory.Utils;
import spark.Request;
import spark.Response;
import spark.Route;

public class DirectoryConfigurationController {

	private DirectoryConfigurationController() {
		super();
	}
	
	public static final Route configuration = (Request request, Response response) -> {
		response.header(HttpHeaders.CONTENT_TYPE, Utils.MIME_JSON);
		response.status(200);
		return Directory.getConfiguration().toJson();
	};
	
	public static final Route configure = (Request request, Response response) -> {
		// Parse and validate service configuration payload
		DirectoryConfiguration newConfiguration = DirectoryConfiguration.serialiseFromJson(request.body());
		// Update global configuration
		Directory.setConfiguration(newConfiguration);
		// setup output
		response.status(200);
		return "";
	};
	
	public static final Route triplestoreConfiguration = (Request request, Response response) -> {
		response.header(HttpHeaders.CONTENT_TYPE, Utils.MIME_JSON);
		response.status(200);
		return Directory.getConfiguration().getTriplestore().toJson();
	};
	
	public static final Route configureTriplestore = (Request request, Response response) -> {		
		// Parse and validate triplestore configuration payload
		TriplestoreConfiguration newTriplestoreConfiguration = TriplestoreConfiguration.serialiseFromJson(request.body());
		// Update global configuration
		DirectoryConfiguration newConfiguration = Directory.getConfiguration();
		newConfiguration.setTriplestore(newTriplestoreConfiguration);
		Directory.setConfiguration(newConfiguration);
		// setup output
		response.status(200);
		return "";	
	};
	
	public static final Route validationConfiguration = (Request request, Response response) -> {
		response.header(HttpHeaders.CONTENT_TYPE, Utils.MIME_JSON);
		response.status(200);
		return Directory.getConfiguration().getValidation().toJson();
	};

	public static final Route configureValidation = (Request request, Response response) -> {
		// Parse and validate service configuration payload
		ValidationConfiguration newValidationConfiguration = ValidationConfiguration.serialiseFromJson(request.body());
		// Update global configuration
		DirectoryConfiguration newConfiguration = Directory.getConfiguration();
		newConfiguration.setValidation(newValidationConfiguration);
		Directory.setConfiguration(newConfiguration);
		// setup output
		response.status(200);
		return "";
	};

	public static final Route serviceConfiguration = (Request request, Response response) -> {
		response.header(HttpHeaders.CONTENT_TYPE, Utils.MIME_JSON);
		response.status(200);
		return Directory.getConfiguration().getService().toJson();
	};
	
	public static final Route configureService = (Request request, Response response) -> {
		// Parse and validate service configuration payload
		ServiceConfiguration newServiceConfiguration = ServiceConfiguration.serialiseFromJson(request.body());
		// Update global configuration
		DirectoryConfiguration newConfiguration = Directory.getConfiguration();
		newConfiguration.setService(newServiceConfiguration);
		Directory.setConfiguration(newConfiguration);
		// setup output
		response.status(200);
		return "";
	};

}
