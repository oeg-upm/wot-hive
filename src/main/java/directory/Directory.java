package directory;

import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import static spark.Spark.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import directory.events.EventsController;
import directory.exceptions.Exceptions;
import directory.exceptions.RemoteException;
import directory.exceptions.SearchJsonPathException;
import directory.exceptions.SearchXPathException;
import directory.exceptions.ThingNotFoundException;
import directory.exceptions.ThingParsingException;
import directory.exceptions.ThingRegistrationException;
import directory.search.JsonPathController;
import directory.search.SparqlController;
import directory.search.XPathController;
import directory.td.ThingsController;
import directory.td.ThingsDAO;
import directory.td.ThingsService;
import spark.Request;
import spark.Response;
import spark.Route;
import wot.jtd.JTD;
import wot.jtd.Vocabulary;

public class Directory {
	
	private static final String DIRECTORY_VERSION = "WoTHive/0.1.0";
	// -- Attributes
	protected static final Logger LOGGER = LoggerFactory.getLogger("");
	public static String DIRECTORY_BASE = "https://oeg.fi.upm.es/directory/";
	private static int port = 9000;
	private static int maxThreads = 8;
	private static int minThreads = 2;
	private static int timeOutMillis = 30000;
	public static boolean shaclValidation = false;
	public static boolean jsonSchemaValidation = false;
	
	// -- Constructor
	private Directory() {
		super();
	}
	
	// -- Methods
	
	private static final void setup() {
		port(port);
		threadPool(maxThreads, minThreads, timeOutMillis);
	}
	
	// Adding swagger: https://serol.ro/posts/2016/swagger_sparkjava/
	public static void main(String[] args) throws UnsupportedEncodingException, IOException {
		LOGGER.info(Utils.WOT_DIRECTORY_LOGO);
		LOGGER.info("WoT Directory service v0.1.0");
		setup();
		JTD.setDefaultRdfNamespace(Directory.DIRECTORY_BASE);
		JTD.removeArrayCompactKey(Vocabulary.SECURITY);
		
		SPARQLEndpoint.setQueryUsingGET(true);
		SPARQLEndpoint.setQuerySparqlEndpoint("http://localhost:4567/sparql");
		SPARQLEndpoint.setUpdateSparqlEndpoint("http://localhost:4567/sparql");
		
		
		path(Path.THINGS, () -> {
		    //before("/*", (q, a) -> LOGGER.info("Received api call"));
			
		    get(		"",    	ThingsController.listing);
		    get(		"/:id",    ThingsController.retrieval);
		    post(	"/",       ThingsController.registrationAnonymous);
		    post(	"",       ThingsController.registrationAnonymous);
		    put(		"/:id",    ThingsController.registrationUpdate);
		    patch(	"/:id",  ThingsController.partialUpdate);
		    delete(	"/:id", ThingsController.deletion);
			exception(ThingNotFoundException.class,      ThingNotFoundException.handleThingNotFoundException);
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
		
		
	
		exception(ThingRegistrationException.class,      Exceptions.handleThingRegistrationException);
		exception(ThingParsingException.class,      Exceptions.handleThingParsingException);
		exception(RemoteException.class,      Exceptions.handleRemoteException);
		exception(Exception.class,      Exceptions.handleException);
		
	
		//createMock();
		//initListeningDNSN();
		//registration();
		
		// Using Route
		notFound((request, response) -> {
			response.type(Utils.MIME_JSON);
			response.status(400);
			response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		    return "{\"message\":\"error\"}";
		});
		

		internalServerError((request, response) -> {
			response.type(Utils.MIME_JSON);
			response.status(500);
			response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		    return "{\"message\":\"error\"}";
		});
		after((request, response) -> {
		    response.header("Server", Directory.DIRECTORY_VERSION);
		    System.out.println(request.requestMethod()+" "+request.pathInfo()+" "+request.contextPath()+" "+request.params());
		});
		
	}
	
	

	
	public static void createMock() {
		String td = "{\n" + 
				"    \"@context\": \"https://www.w3.org/2019/wot/td/v1\",\n" + 
				"    \"title\": \"MyLampThing\",\n" + 
				"    \"securityDefinitions\": {\n" + 
				"        \"basic_sc\": {\"scheme\": \"basic\", \"in\":\"header\"}\n" + 
				"    },\n" + 
				"    \"security\": [\"basic_sc\"],\n" + 
				"    \"properties\": {\n" + 
				"        \"status\" : {\n" + 
				"            \"type\": \"string\",\n" + 
				"            \"forms\": [{\"href\": \"https://mylamp.example.com/status\"}]\n" + 
				"        }\n" + 
				"    },\n" + 
				"    \"actions\": {\n" + 
				"        \"toggle\" : {\n" + 
				"            \"forms\": [{\"href\": \"https://mylamp.example.com/toggle\"}]\n" + 
				"        }\n" + 
				"    },\n" + 
				"    \"events\":{\n" + 
				"        \"overheating\":{\n" + 
				"            \"data\": {\"type\": \"string\"},\n" + 
				"            \"forms\": [{\n" + 
				"                \"href\": \"https://mylamp.example.com/oh\",\n" + 
				"                \"subprotocol\": \"longpoll\"\n" + 
				"            }]\n" + 
				"        }\n" + 
				"    }\n" + 
				"}";
		for(int i =0; i< 100000; i++)
			ThingsService.registerJsonThingAnonymous(td);
	}
	
	private static final String DNSN_NAME = "_directory._sub._wot";
	private static void initListeningDNSN() {
		try {
			// Create a JmDNS instance
			JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

			// Add a service listener
			jmdns.addServiceListener(DNSN_NAME, new SampleListener());

			// Wait a bit
			Thread.sleep(30000);
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("Service resolved: " + event.getInfo());
        }
    }
	
	private static void registration()  {
	     try {
	            // Create a JmDNS instance
	            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

	            // Register a service
	            ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", "wot", port, "path=index.html");
	            jmdns.registerService(serviceInfo);

	            // Wait a bit
	            Thread.sleep(25000);

	            // Unregister all services
	            jmdns.unregisterAllServices();

	        } catch (IOException e) {
	            System.out.println(e.getMessage());
	        } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	

}

