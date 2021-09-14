package directory.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import directory.exceptions.ConfigurationException;

public class ValidationConfiguration extends AbstractConfiguration{

	// -- Attributes
	private boolean enableShaclValidation;
	private boolean enableJsonSchemaValidation;
	private String schemaFile;
	private String shapesFile;

	// -- Constructors
	public ValidationConfiguration() {
		super();
	}

	public ValidationConfiguration(boolean enableShaclValidation, boolean enableJsonSchemaValidation, String schemaFile,
			String shapesFile) {
		super();
		this.enableShaclValidation = enableShaclValidation;
		this.enableJsonSchemaValidation = enableJsonSchemaValidation;
		this.schemaFile = schemaFile;
		this.shapesFile = shapesFile;
	}

	// -- Getters & Setters

	public boolean isEnableShaclValidation() {
		return enableShaclValidation;
	}

	public void setEnableShaclValidation(boolean enableShaclValidation) {
		this.enableShaclValidation = enableShaclValidation;
	}

	public boolean isEnableJsonSchemaValidation() {
		return enableJsonSchemaValidation;
	}

	public void setEnableJsonSchemaValidation(boolean enableJsonSchemaValidation) {
		this.enableJsonSchemaValidation = enableJsonSchemaValidation;
	}

	public String getSchemaFile() {
		return schemaFile;
	}

	public void setSchemaFile(String schemaFile) {
		this.schemaFile = schemaFile;
	}

	public String getShapesFile() {
		return shapesFile;
	}

	public void setShapesFile(String shapesFile) {
		this.shapesFile = shapesFile;
	}

	// Serialization methods

	public static ValidationConfiguration serialiseFromJson(String rawJson) {
		JsonObject body = null;
		try {
			body = (new Gson()).fromJson(rawJson, JsonObject.class);
		} catch (Exception e) {
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_3, e.toString());
		}
		if (body == null)
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_1,
					"A valid JSON configuration must be provided containing the following mandatory keys: \"enableShaclValidation\",  \"enableJsonSchemaValidation\", \"schemaFile\", and \"shapesFile\". For instance '{\"enableShaclValidation\":true,\"enableJsonSchemaValidation\":true,\"schemaFile\":\"./schema.json\",\"shapesFile\":\"./shape.ttl\"}'");
		//
		validatePayload(body, "enableShaclValidation", ConfigurationException.EXCEPTION_CODE_2,
				"Provided JSON lacks of mandatory key \"enableShaclValidation\" that activates the validation of Thing Descriptions through SHACL shapes");
		validatePayload(body, "enableJsonSchemaValidation", ConfigurationException.EXCEPTION_CODE_2,
				"Provided JSON lacks of mandatory key \"enableJsonSchemaValidation\"  that activates the validation of Thing Descriptions through JSON Schema");
		validatePayload(body, "schemaFile", ConfigurationException.EXCEPTION_CODE_2,
				"Provided JSON lacks of mandatory key \"schemaFile\" pointing to a local JSON Schema file for validating Thing Descriptions");
		validatePayload(body, "shapesFile", ConfigurationException.EXCEPTION_CODE_2,
				"Provided JSON lacks of mandatory key \"shapesFile\" pointing to a local SHACL file in turtle for validating Thing Descriptions");
		
		return (new Gson()).fromJson(body, ValidationConfiguration.class);
	}




}
