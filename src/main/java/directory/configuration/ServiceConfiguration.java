package directory.configuration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import directory.exceptions.ConfigurationException;

public class ServiceConfiguration extends AbstractConfiguration{

	// -- Attributes
	
	private String directoryURIBase ;
	private int port;
	private int maxThreads;
	private int minThreads;
	private int timeOutMillis;
	private int eventsSize;
	private String eventsFile;
	
	// -- Constructors
	public ServiceConfiguration() {
		super();
	}
	
	public ServiceConfiguration(String directoryURIBase, int port, int maxThreads, int minThreads, int timeOutMillis, int eventsSize, String eventsFile) {
		super();
		this.directoryURIBase = directoryURIBase;
		this.port = port;
		this.maxThreads = maxThreads;
		this.minThreads = minThreads;
		this.timeOutMillis = timeOutMillis;
		this.eventsSize = eventsSize;
		this.eventsFile = eventsFile;
	}

	// -- Getters & Setters

	public String getDirectoryURIBase() {
		return directoryURIBase;
	}

	public void setDirectoryURIBase(String directoryURIBase) {
		this.directoryURIBase = directoryURIBase;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getMaxThreads() {
		return maxThreads;
	}

	public void setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
	}

	public int getMinThreads() {
		return minThreads;
	}

	public void setMinThreads(int minThreads) {
		this.minThreads = minThreads;
	}

	public int getTimeOutMillis() {
		return timeOutMillis;
	}

	public void setTimeOutMillis(int timeOutMillis) {
		this.timeOutMillis = timeOutMillis;
	}
		
	public int getEventsSize() {
		return eventsSize;
	}

	public void setEventsSize(int eventsSize) {
		this.eventsSize = eventsSize;
	}
	
	public String getEventsFile() {
		return eventsFile;
	}

	public void setEventsFile(String eventsFile) {
		this.eventsFile = eventsFile;
	}

	// Serialization methods
	
	public static ServiceConfiguration serialiseFromJson(String rawJson) {
		JsonObject body = null;
		try {
			body = (new Gson()).fromJson(rawJson, JsonObject.class);
		}catch(Exception e) {
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_3, e.toString());
		}
		if (body==null)
			throw new ConfigurationException(ConfigurationException.EXCEPTION_CODE_1, "A valid JSON configuration must be provided containing the following mandatory keys: \"directoryURIBase\",  \"port\", \"maxThreads\", \"minThreads\", and \"timeOutMillis\". For instance '{\"directoryURIBase\":\"https://oeg.fi.upm.es/wothive/\",\"port\":8080,\"maxThreads\":200,\"minThreads\":2,\"timeOutMillis\":30000}'");	
		//  
		validatePayload( body, "directoryURIBase", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"directoryURIBase\" used as base URI for the Thing Descriptions");
		validatePayload( body, "port", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"port\" indicating the port where this service runs");
		validatePayload( body, "maxThreads", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"maxThreads\" indicating the maximum number of threads that the service can use");
		validatePayload( body, "minThreads", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"minThreads\" indicating the minimum number of threads that the service can use");
		validatePayload( body, "timeOutMillis", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"timeOutMillis\" indicating the requests time out of the service");
		validatePayload( body, "eventsSize", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"eventsSize\" indicating the maximum number of events to be stored");
		validatePayload( body, "eventsFile", ConfigurationException.EXCEPTION_CODE_2, "Provided JSON lacks of mandatory key \"eventsFile\" indicating the file where the historical events will be stored");

		
		return (new Gson()).fromJson(body, ServiceConfiguration.class);
	}
	
	

	
}
