package directory.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "configuration")
public class DirectoryConfiguration {

	// -- Attributes
	@DatabaseField(id=true, dataType = DataType.INTEGER)
	private int id;
	@DatabaseField
	private String directoryURIBase ;
	@DatabaseField
	private int port;
	@DatabaseField
	private int maxThreads;
	@DatabaseField
	private int minThreads;
	@DatabaseField
	private int timeOutMillis;
	@DatabaseField
	private boolean enableShaclValidation;
	@DatabaseField
	private boolean enableJsonSchemaValidation;
	@DatabaseField
	private String schemaFile;
	@DatabaseField
	private String shapesFile;
	@DatabaseField
	private boolean enableSecurity = false;
	
	// -- Constructor
	public DirectoryConfiguration() {
		super();
	}

	public static DirectoryConfiguration parseConfiguration(String str) {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.fromJson(str, DirectoryConfiguration.class);
	}
	
	public String toJson() {
		GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		return gson.toJson(this);
	}
	
	// -- Methods
	
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

	public boolean isEnableSecurity() {
		return enableSecurity;
	}

	public void setEnableSecurity(boolean enableSecurity) {
		this.enableSecurity = enableSecurity;
		
	}

	public String getSchemaFile() {
		return schemaFile;
	}

	public String getShapesFile() {
		return shapesFile;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setSchemaFile(String schemaFile) {
		this.schemaFile = schemaFile;
	}

	public void setShapesFile(String shapesFile) {
		this.shapesFile = shapesFile;
	}
	
	
}
