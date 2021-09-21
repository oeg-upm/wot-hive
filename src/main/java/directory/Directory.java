package directory;

import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.patch;
import static spark.Spark.path;
import static spark.Spark.after;

import static spark.Spark.notFound;
import static spark.Spark.internalServerError;
import static spark.Spark.threadPool;

import java.io.File;
import java.util.List;

import static spark.Spark.port;

import org.apache.jena.ext.com.google.common.io.Files;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import directory.configuration.DirectoryConfiguration;
import directory.configuration.DirectoryConfigurationController;
import directory.events.DirectoryEvent;
import directory.events.EventsController;
import directory.exceptions.ConfigurationException;
import directory.exceptions.Exceptions;
import directory.exceptions.RemoteException;
import directory.exceptions.SearchJsonPathException;
import directory.exceptions.SearchXPathException;
import directory.exceptions.ThingNotFoundException;
import directory.exceptions.ThingParsingException;
import directory.exceptions.ThingRegistrationException;
import directory.exceptions.ThingValidationException;
import directory.search.JsonPathController;
import directory.search.SparqlController;
import directory.search.XPathController;
import directory.things.ThingsController;
import directory.things.ThingsDAO;
import directory.things.ThingsMapper;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Service;
import wot.jtd.JTD;
import wot.jtd.Vocabulary;

public class Directory {

	// -- Attributes
	protected static DirectoryConfiguration configuration;
	public static final File CONFIGURATION_FILE = new File("./configuration.json");
	public static final Logger LOGGER = LoggerFactory.getLogger(Directory.class);
	public static final String MARKER_DEBUG_SPARQL = "[REMOTESPARQL]";
	public static final String MARKER_DEBUG = "[DIRECTORY]";

	// -- Constructor
	private Directory() {
		super();
	}

	// -- Main method
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		setup();
		//SparkSwagger.of()
		get("/.well-known/wot-thing-description", Directory.getSelfDescription);
		
		path("/configuration", () -> {
			get("", DirectoryConfigurationController.configuration);
			get("/service", DirectoryConfigurationController.serviceConfiguration);
			get("/triplestore", DirectoryConfigurationController.triplestoreConfiguration);
			get("/validation", DirectoryConfigurationController.validationConfiguration);
			post("", DirectoryConfigurationController.configure);
			post("/service", DirectoryConfigurationController.configureService);
			post("/triplestore", DirectoryConfigurationController.configureTriplestore);
			post("/validation", DirectoryConfigurationController.configureValidation);
			exception(ConfigurationException.class,ConfigurationException.handleConfigurationException);
		});
		
		
		path("/api", () -> {
			path("/things", () -> {
				get("", ThingsController.listing);
				get("/:id", ThingsController.retrieval);
				post("/", ThingsController.registrationAnonymous);
				post("", ThingsController.registrationAnonymous);
				put("/:id", ThingsController.registrationUpdate);
				patch("/:id", ThingsController.partialUpdate);
				delete("/:id", ThingsController.deletion);
				exception(ThingNotFoundException.class, ThingNotFoundException.handleThingNotFoundException);
				exception(ThingValidationException.class, ThingValidationException.handleException);
				exception(ThingRegistrationException.class, Exceptions.handleThingRegistrationException);
				exception(ThingParsingException.class, Exceptions.handleThingParsingException);
				exception(RemoteException.class, RemoteException.handleRemoteException);
				exception(Exception.class, Exceptions.handleException);
			});
			path("/search", () -> {
				get("/jsonpath", JsonPathController.solveJsonPath);
				exception(SearchJsonPathException.class, SearchJsonPathException.handleSearchJsonPathException);
				get("/xpath", XPathController.solveXPath);
				exception(SearchXPathException.class, SearchXPathException.handleSearchXPathException);
				get("/sparql", SparqlController.solveSparqlQuery);
				post("/sparql", SparqlController.solveSparqlQuery);
			});
			path("/events", () -> {
				get("", EventsController.subscribe);
				get("/create", EventsController.subscribeCreate);
				get("/update", EventsController.subscribeUpdate);
				get("/delete", EventsController.subscribeDelete);
			});
		});

		// Unmatched Routes
		notFound((Request request, Response response) ->  handleUnmatchedRoutes(request, response, 400));
		internalServerError((Request request, Response response) ->  handleUnmatchedRoutes(request, response, 500));

		after((request, response) -> {
			response.header("Server", Utils.DIRECTORY_VERSION);
			String logStr = Utils.buildMessage(request.requestMethod(), " ", request.pathInfo());
			Directory.LOGGER.info(logStr);
		});
		
		
	}
	
	public static final Route getSelfDescription = (Request request, Response response) -> {
		try {
			String format = request.headers(Utils.HEADER_ACCEPT);
			JsonObject description = JTD.parseJson(Utils.readFile(new File("self-description.json")));
			String id = 	Utils.buildMessage("http://",request.raw().getServerName(), request.uri());
			if(request.port()!=80) 
				id = Utils.buildMessage("http://",request.raw().getServerName(), ":", String.valueOf(request.port()), request.uri());

			description.addProperty("@id", id);
			if(format!=null && format.equals(Utils.MIME_TURTLE)) {
				response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_TURTLE);
				Model model = JTD.toRDF(description);
				model.write(response.raw().getOutputStream(), "TURTLE");
			}else {
				response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_THING);
			}
			return description;
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return "";
		};

	private static String handleUnmatchedRoutes(Request request, Response response, int status) {
		response.type(Utils.MIME_JSON);
		response.status(status);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		if (Directory.LOGGER.isDebugEnabled()) {
			String logStr = Utils.buildMessage(request.requestMethod(), " ", request.pathInfo(), " - ",
					String.valueOf(status));
			Directory.LOGGER.debug(MARKER_DEBUG, logStr);
		}
		return "{\"message\":\"error\"}";
	}
	


	// -- Methods

	public static DirectoryConfiguration getConfiguration() {
		return configuration;
	}
	
	public static void setConfiguration(DirectoryConfiguration newConfiguration) {
		configuration = newConfiguration;
		JTD.setDefaultRdfNamespace(configuration.getService().getDirectoryURIBase());
		// Persist new configuration
		try {
			Files.write(configuration.toJson().getBytes(), Directory.CONFIGURATION_FILE);
		}catch(Exception e) {
			LOGGER.error(e.toString());
		}
	}
	

	private static final void setup() {
		// logs configuration
		String log4jConfPath = "./log4j.properties";
		PropertyConfigurator.configure(log4jConfPath);
		// JTD extra configuration
		JTD.removeArrayCompactKey(Vocabulary.SECURITY);
		// Configuration: creates default or load existing
		try {
			DirectoryConfiguration newConfiguration = DirectoryConfiguration.syncConfiguration();
			setConfiguration(newConfiguration);
			// Apply service configuration only-once
			
			port(configuration.getService().getPort());
			threadPool(configuration.getService().getMaxThreads(), configuration.getService().getMinThreads(),
					configuration.getService().getTimeOutMillis());
		} catch (Exception e) {
			LOGGER.error(e.toString());
			System.exit(-1);
		}
		// Show service info
		LOGGER.info(Utils.WOT_DIRECTORY_LOGO);
		LOGGER.info(Utils.DIRECTORY_VERSION);
	}
}
