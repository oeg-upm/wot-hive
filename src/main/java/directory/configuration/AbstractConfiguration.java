package directory.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import directory.exceptions.ConfigurationException;

public abstract class AbstractConfiguration {

	protected static void validatePayload(JsonObject body, String key, String code, String exceptionMesage) {
		if (!body.has(key))
			throw new ConfigurationException(code, exceptionMesage);
	}
	
	public String toJson() {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.toJson(this);
	}
	
}
