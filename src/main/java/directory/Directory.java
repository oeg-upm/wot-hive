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
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;

import static spark.Spark.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import directory.configuration.DirectoryConfiguration;
import directory.events.EventsController;
import directory.exceptions.DirectoryAutenticationException;
import directory.exceptions.DirectoryAuthorizationException;
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
import directory.security.CredentialsController;
import directory.security.SecurityController;
import directory.storage.CredentialsSQLFactory;
import directory.things.ThingsController;
import directory.things.store.SPARQLEndpoint;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import wot.jtd.JTD;
import wot.jtd.Vocabulary;

public class Directory {
	
	private static final String DIRECTORY_VERSION = "WoTHive/0.1.0";
	// -- Attributes
	public static final Logger LOGGER = LoggerFactory.getLogger("");
	public static final String MARKER_DEBUG_SECURITY = "[SECURITY]";
	public static final String MARKER_DEBUG_SPARQL = "[REMOTESPARQL]";
	public static final String MARKER_DEBUG = "[DIRECTORY]";
	public static final String MARKER_DEBUG_SQL = "[SQL]";
	public static DirectoryConfiguration configuration;
	
	// -- Constructor
	private Directory() {
		super();
	}
	
	// -- Methods
	
	private static DirectoryConfiguration defaultConfiguration() {
		DirectoryConfiguration configuration = new DirectoryConfiguration();
		configuration.setId(1);
		configuration.setDirectoryURIBase("https://oeg.fi.upm.es/wothive/");
		configuration.setPort(9000);
		configuration.setMaxThreads(200);
		configuration.setMinThreads(2);
		configuration.setTimeOutMillis(30000);
		configuration.setEnableShaclValidation(true);
		configuration.setEnableJsonSchemaValidation(true);
		configuration.setSchemaFile("./schema.json");
		configuration.setShapesFile("./shape.ttl");
		configuration.setEnableSecurity(true);
		return configuration;
	}
	
	private static final void setup() {
		Directory.configuration = defaultConfiguration();
		DirectoryConfiguration configuration = defaultConfiguration();
        // create a connection source to our database
		try {
			
	        ConnectionSource connectionSource = new JdbcConnectionSource("jdbc:h2:file:./h2");
	        Dao<DirectoryConfiguration, String> configurationDao = DaoManager.createDao(connectionSource, DirectoryConfiguration.class);
	        TableUtils.createTableIfNotExists(connectionSource, DirectoryConfiguration.class);
	        //configurationDao.create(configuration);
	        configurationDao.update(configuration);
	        
	        System.out.println(configurationDao.idExists("1"));
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		
		port(configuration.getPort());
		threadPool(configuration.getMaxThreads(), configuration.getMinThreads(), configuration.getTimeOutMillis());
		JTD.setDefaultRdfNamespace(configuration.getDirectoryURIBase());
		JTD.removeArrayCompactKey(Vocabulary.SECURITY);
		SPARQLEndpoint.setQueryUsingGET(true);
		SPARQLEndpoint.setQuerySparqlEndpoint("http://localhost:4567/sparql");
		SPARQLEndpoint.setUpdateSparqlEndpoint("http://localhost:4567/sparql");
	}
	
	// Adding swagger: https://serol.ro/posts/2016/swagger_sparkjava/
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws ClassNotFoundException  {
		Class.forName("org.h2.Driver");
		LOGGER.info(Utils.WOT_DIRECTORY_LOGO);
		LOGGER.info("WoTHive service v0.1.0");
		setup();
		
		before("*", (request, response) -> CredentialsSQLFactory.dbStatus());
		before("*", SecurityController.filterRequests);
		exception(DirectoryAuthorizationException.class,      DirectoryAuthorizationException.handleException);
		
		path("/api", () -> {
			path("/credentials", () -> {
				before( "", CredentialsController.filterRequests);
				get(    "", CredentialsController.retrieveCredentials);
				post(   "", CredentialsController.updateCredentials);
				post(   "/jwt", SecurityController.generateToken);
				exception(DirectoryAutenticationException.class,      DirectoryAutenticationException.handleException);
			});
			path("/things", () -> {
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
				exception(RemoteException.class,      RemoteException.handleRemoteException);
				exception(Exception.class,      Exceptions.handleException);
			});
			path("/search", () -> {
				get("/jsonpath",  JsonPathController.solveJsonPath);
				exception(SearchJsonPathException.class,     SearchJsonPathException.handleSearchJsonPathException);
				get("/xpath",    XPathController.solveXPath);
				exception(SearchXPathException.class,     SearchXPathException.handleSearchXPathException);
				get("/sparql",   SparqlController.solveSparqlQuery);
				post("/sparql",  SparqlController.solveSparqlQuery);
			});
			path("/events", () -> {
			    get(		"",    	EventsController.subscribe);
			});
		
		});
		
		//initListeningDNSN();
		//registration();
	
		// Unmatched Routes
		notFound(handle400Routes);
		internalServerError(Directory.handle500Routes);
		
		after((request, response) -> {
		    response.header("Server", Directory.DIRECTORY_VERSION);
			String logStr = Utils.buildMessage(request.requestMethod()," ",request.pathInfo());
		    Directory.LOGGER.info(logStr);
		});
	}
	
	
	private static final Route handle400Routes = (Request request, Response response) -> {		
		return handleUnmatchedRoutes(request, response, 400);
	};
	private static final Route handle500Routes = (Request request, Response response) -> {		
		return handleUnmatchedRoutes(request, response, 500);
	};
	
	private static String handleUnmatchedRoutes(Request request, Response response, int status) {
		response.type(Utils.MIME_JSON);
		response.status(status);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		if(Directory.LOGGER.isDebugEnabled()) {
			String logStr = Utils.buildMessage(request.requestMethod()," ",request.pathInfo()," - ", String.valueOf(status));
			Directory.LOGGER.debug(MARKER_DEBUG, logStr);
		}
	    return "{\"message\":\"error\"}";
	}
}

