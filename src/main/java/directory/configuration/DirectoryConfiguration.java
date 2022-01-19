package directory.configuration;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import directory.Directory;
import directory.exceptions.ConfigurationException;

public class DirectoryConfiguration extends AbstractConfiguration{

	// -- Attributes

	private TriplestoreConfiguration triplestore;
	private ServiceConfiguration service;
	private ValidationConfiguration validation;

	// -- Constructor
	public DirectoryConfiguration() {
		super();

	}

	public DirectoryConfiguration(TriplestoreConfiguration triplestore, ServiceConfiguration service,
			ValidationConfiguration validation) {
		super();
		this.triplestore = triplestore;
		this.service = service;
		this.validation = validation;
	}

	// -- Methods

	public TriplestoreConfiguration getTriplestore() {
		return triplestore;
	}

	public void setTriplestore(TriplestoreConfiguration triplestore) {
		this.triplestore = triplestore;
	}

	public ServiceConfiguration getService() {
		return service;
	}

	public void setService(ServiceConfiguration service) {
		this.service = service;
	}

	public ValidationConfiguration getValidation() {
		return validation;
	}

	public void setValidation(ValidationConfiguration validation) {
		this.validation = validation;
	}

	

	// -- Ancillary methods
	private static final String DEFAULT_TRIPLESTORE_ENDPOINT = "http://localhost:3030/sparql";
	

	public DirectoryConfiguration createDefault() {
		try {
			this.triplestore = new TriplestoreConfiguration(DEFAULT_TRIPLESTORE_ENDPOINT, DEFAULT_TRIPLESTORE_ENDPOINT,
					true);
			this.service = new ServiceConfiguration("https://oeg.fi.upm.es/wothive/", 9000, 200, 2, 30000, 100, "./events.json");
			this.validation = new ValidationConfiguration(true, true, "./schema.json", "./shape.ttl");
		} catch (Exception e) {
			Directory.LOGGER.error(e.toString());
		}
		return this;
	}

	// -- Ancillary static methods

	public static DirectoryConfiguration serialiseFromJson(String rawJson) {
		JsonObject body = null;
		try {
			body = (new Gson()).fromJson(rawJson, JsonObject.class);
		} catch (Exception e) {
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_3, e.toString());
		}
		if (body == null)
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_1,
					"A valid JSON configuration must be provided. For example: {\"triplestore\":{\"updateEnpoint\":\"http://localhost:3030/sparql\",\"queryEnpoint\":\"http://localhost:3030/sparql\",\"queryUsingGET\":true},\"service\":{\"directoryURIBase\":\"https://oeg.fi.upm.es/wothive/\",\"port\":8080,\"maxThreads\":200,\"minThreads\":2,\"timeOutMillis\":30000},\"validation\":{\"enableShaclValidation\":true,\"enableJsonSchemaValidation\":true,\"schemaFile\":\"./schema.json\",\"shapesFile\":\"./shape.ttl\"}}");
		
		// Validates configuration payload and nested JSONs
		validatePayload(body, "triplestore", ConfigurationException.EXCEPTION_CODE_2,
				"Provided JSON lacks of mandatory key \"triplestore\" with a triplestore configuration json. For instance'{\"updateEnpoint\":\"http://localhost:3030/sparql\",\"queryEnpoint\":\"http://localhost:3030/sparql\",\"queryUsingGET\":true}'");
		TriplestoreConfiguration.serialiseFromJson(body.get("triplestore").getAsJsonObject().toString());
		
		validatePayload(body, "service", ConfigurationException.EXCEPTION_CODE_2,
				"Provided JSON lacks of mandatory key \"service\" with a service configuration json. For instance '{\"directoryURIBase\":\"https://oeg.fi.upm.es/wothive/\",\"port\":8080,\"maxThreads\":200,\"minThreads\":2,\"timeOutMillis\":30000}'");
		ServiceConfiguration.serialiseFromJson(body.get("service").getAsJsonObject().toString());

		validatePayload(body, "validation", ConfigurationException.EXCEPTION_CODE_2,
				"Provided JSON lacks of mandatory key \"validation\" with a validation configuration json. For instance '{\"enableShaclValidation\":true,\"enableJsonSchemaValidation\":true,\"schemaFile\":\"./schema.json\",\"shapesFile\":\"./shape.ttl\"}'");
		ValidationConfiguration.serialiseFromJson(body.get("validation").getAsJsonObject().toString());

		return (new Gson()).fromJson(body, DirectoryConfiguration.class);
	}

	public static DirectoryConfiguration syncConfiguration() throws IOException {
		DirectoryConfiguration configuration = null;
		if (!Directory.CONFIGURATION_FILE.exists()) {
			// Create default on-memory
			configuration = (new DirectoryConfiguration().createDefault());
		} else {
			// read and load on-memory
			Reader reader = new FileReader(Directory.CONFIGURATION_FILE);
			Writer writer = new StringWriter();
			reader.transferTo(writer);
			reader.close();
			configuration = DirectoryConfiguration.serialiseFromJson(writer.toString());
			writer.close();
		}
		return configuration;
	}
	
	

}
