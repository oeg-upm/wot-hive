package directory;

import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.patch;
import static spark.Spark.path;
import static spark.Spark.after;
import static spark.Spark.redirect;

import static spark.Spark.notFound;
import static spark.Spark.internalServerError;
import static spark.Spark.threadPool;

import java.io.File;
import static spark.Spark.port;

import org.apache.jena.ext.com.google.common.io.Files;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import directory.configuration.DirectoryConfiguration;
import directory.configuration.DirectoryConfigurationController;
import directory.events.EventsController;
import directory.exceptions.ConfigurationException;
import directory.exceptions.NotFoundException;
import directory.exceptions.RemoteSparqlEndpointException;
import directory.exceptions.SearchJsonPathException;
import directory.exceptions.SearchSparqlException;
import directory.exceptions.ThingException;
import directory.exceptions.ThingValidationException;
import directory.search.JsonPathController;
import directory.search.SparqlController;
import directory.search.XPathController;
import directory.things.ThingsController;
import directory.triplestore.Sparql;
import spark.Request;
import spark.Response;
import spark.Route;

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
		exception(SelfDescriptionException.class,SelfDescriptionException.handleSelfDescriptionException);
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
			
			path("/search", () -> {
				get("/jsonpath", JsonPathController.solveJsonPath);
				exception(SearchJsonPathException.class, SearchJsonPathException.handleSearchJsonPathException);
				get("/xpath", XPathController.solveXPath);
				get("/sparql", SparqlController.solveSparqlQuery);
				post("/sparql", SparqlController.solveSparqlQuery);
				exception(SearchSparqlException.class, SearchSparqlException.handleSearchSparqlException);

			});
			path("/events", () -> {
				get("", EventsController.subscribe);
				get("/thing_created", EventsController.subscribeCreate);
				get("/thing_updated", EventsController.subscribeUpdate);
				get("/thing_deleted", EventsController.subscribeDelete);
			});
			path("/things", () -> {
				get("", ThingsController.listing);
				get("/", ThingsController.listing);
				get("/:id", ThingsController.retrieval);
				post("/", ThingsController.registrationAnonymous);
				post("", ThingsController.registrationAnonymous);
				put("/:id", ThingsController.registrationUpdate);
				patch("/:id", ThingsController.partialUpdate);
				delete("/:id", ThingsController.deletion);
				exception(ThingValidationException.class, ThingValidationException.handleException);
				exception(ThingException.class, ThingException.handleThingRegistrationException);
				exception(RemoteSparqlEndpointException.class, RemoteSparqlEndpointException.handleRemoteException);
				exception(NotFoundException.class, NotFoundException.handleNotFoundExceptionException);
				exception(Exception.class, Utils.handleException);
			});
		});
		
		redirect.get("", "/api/things");
		redirect.get("/", "/api/things");
		
		// Unmatched Routes
		notFound((Request request, Response response) ->  handleUnmatchedRoutes(request, response, 400));
		internalServerError((Request request, Response response) ->  handleUnmatchedRoutes(request, response, 500));

		after((request, response) -> {
			response.header("Server", Utils.DIRECTORY_VERSION);
			String logStr = Utils.buildMessage(request.requestMethod(), " ", request.pathInfo());
			Directory.LOGGER.info(logStr);
		});
	
	}
	
//	public static final Route redirect = (Request request, Response response) -> {
//		//Redirect.
//	}
	public static final Route getSelfDescription = (Request request, Response response) -> {
		try {
			String format = request.headers(Utils.HEADER_ACCEPT);
			JsonObject description = Utils.toJson(Utils.readFile(new File("self-description.json")));
			String id = 	Utils.buildMessage("http://",request.raw().getServerName(), request.uri());
			if(request.port()!=80) 
				id = Utils.buildMessage("http://",request.raw().getServerName(), ":", String.valueOf(request.port()), request.uri());

			description.addProperty("@id", id);
			if(format!=null && format.equals(Utils.MIME_TURTLE)) {
				response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_TURTLE);
				Model model = ModelFactory.createDefaultModel();//TODO:.toRDF(description);
				model.write(response.raw().getOutputStream(), "TURTLE");
			}else {
				response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_THING);
			}
			return description;
			
		}catch(Exception e) {
			throw new SelfDescriptionException(e.toString());
		}
		
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
		// Persist new configuration
		try {
			Files.write(configuration.toJson().getBytes(), Directory.CONFIGURATION_FILE);
		}catch(Exception e) {
			e.printStackTrace();
			LOGGER.error(e.toString());
		}
	}
	

	private static final void setup() {
		// logs configuration
		String log4jConfPath = "./log4j.properties";
		PropertyConfigurator.configure(log4jConfPath);
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
