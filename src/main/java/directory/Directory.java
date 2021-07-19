package directory;

import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.halt;
import static spark.Spark.put;
import static spark.Spark.patch;
import static spark.Spark.path;
import static spark.Spark.before;
import static spark.Spark.after;

import static spark.Spark.notFound;
import static spark.Spark.internalServerError;
import static spark.Spark.threadPool;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static spark.Spark.port;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import directory.events.EventsController;
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
import directory.users.SQLFactory;
import directory.users.SecurityController;
import spark.Spark;
import things.ThingsController;
import wot.jtd.JTD;
import wot.jtd.Vocabulary;

public class Directory {
	
	private static final String DIRECTORY_VERSION = "WoTHive/0.1.0";
	// -- Attributes
	public static final Logger LOGGER = LoggerFactory.getLogger("");
	public static String DIRECTORY_BASE = "https://oeg.fi.upm.es/directory/";
	public static int port = 9000;
	private static int maxThreads = 8;
	private static int minThreads = 2;
	private static int timeOutMillis = 30000;
	public static boolean shaclValidation = true;
	public static boolean jsonSchemaValidation = true;
	public static final String SCHEMA_FILE = "./schema.json";
	public static final String SHAPE_FILE = "./shape.ttl";
	protected static String JWT_SALT = "12345678901234567890123456789012"; // Security

	 
	
	// -- Constructor
	private Directory() {
		super();
	}
	
	// -- Methods
	
	private static final void setup() {
		port(port);
		threadPool(maxThreads, minThreads, timeOutMillis);
		JTD.setDefaultRdfNamespace(Directory.DIRECTORY_BASE);
		JTD.removeArrayCompactKey(Vocabulary.SECURITY);
		SPARQLEndpoint.setQueryUsingGET(true);
		SPARQLEndpoint.setQuerySparqlEndpoint("http://localhost:4567/sparql");
		SPARQLEndpoint.setUpdateSparqlEndpoint("http://localhost:4567/sparql");
	}
	
	// Adding swagger: https://serol.ro/posts/2016/swagger_sparkjava/
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		LOGGER.info(Utils.WOT_DIRECTORY_LOGO);
		LOGGER.info("WoTHive service v0.1.0");
		Spark.staticFiles.location("/public");
		setup();
		
		
		before("*", (request, response) -> {
			SQLFactory.dbStatus(); // Assures that db is always correct
		});
		
		// get("/login", SecurityController.login);
		post("/security/login", SecurityController.login);
		// WoT API (secured)
		before("/api/*", SecurityController.filterRequests);
		path("/api", () -> {
			
			path(Path.THINGS, () -> {
			    get(		"",    	ThingsController.listing);
			    get(		"/:id",    ThingsController.retrieval);
			    post(	"/",       ThingsController.registrationAnonymous);
			    post(	"",       ThingsController.registrationAnonymous);
			    put(		"/:id",    ThingsController.registrationUpdate);
			    patch(	"/:id",  ThingsController.partialUpdate);
			    delete(	"/:id", ThingsController.deletion);
				exception(ThingNotFoundException.class,      ThingNotFoundException.handleThingNotFoundException);
				exception(ThingValidationException.class,      ThingValidationException.handleException);
				exception(ThingRegistrationException.class,      Exceptions.handleThingRegistrationException);
				exception(ThingParsingException.class,      Exceptions.handleThingParsingException);
				exception(RemoteException.class,      Exceptions.handleRemoteException);
				exception(Exception.class,      Exceptions.handleException);
			});
			path(Path.EVENTS, () -> {
			    get(		"",    	EventsController.subscribe);
			});
			path(Path.SEARCH, () -> {
				get("/jsonpath",  JsonPathController.solveJsonPath);
				exception(SearchJsonPathException.class,     SearchJsonPathException.handleSearchJsonPathException);
				get("/xpath",    XPathController.solveXPath);
				exception(SearchXPathException.class,     SearchXPathException.handleSearchXPathException);
				get("/sparql",   SparqlController.solveSparqlQuery);
				post("/sparql",  SparqlController.solveSparqlQuery);
			});
			
		
		});
		
		//initListeningDNSN();
		//registration();
	
		// Using Route
		notFound((request, response) -> {
			response.type(Utils.MIME_JSON);
			response.status(400);
			response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
			System.out.println(request.requestMethod()+" "+request.pathInfo()+ " - 400");
			return "{\"message\":\"error\"}";
		});

		internalServerError((request, response) -> {
			response.type(Utils.MIME_JSON);
			response.status(500);
			response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
			System.out.println(request.requestMethod()+" "+request.pathInfo()+ " - 500");
		    return "{\"message\":\"error\"}";
		});
		
		after((request, response) -> {
		    response.header("Server", Directory.DIRECTORY_VERSION);
		    System.out.println(request.requestMethod()+" "+request.pathInfo());
		});
		
	}
	
}

