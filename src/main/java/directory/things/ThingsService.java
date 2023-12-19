package directory.things;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import directory.Utils;
import directory.events.DirectoryEvent;
import directory.events.EventsController;
import directory.exceptions.NotFoundException;
import directory.exceptions.RemoteSparqlEndpointException;
import directory.exceptions.ThingException;
import directory.triplestore.Sparql;

public class ThingsService {

	// -- Attributes
	private static final String MANAGEMENT_GRAPH = "hive:management:things";
	private static final String NAMEDGRAPH_PREFIX = "graph:";

	// errors
	private static final String EXCEPTION_MSG_NOT_FOUND = "Requested Thing not found";
	private static final String EXCEPTION_MSG_ALREADY_EXISTS = "A Thing with the provided id already exists";
	
	// for method: exists 
	private static final String ASK_PREAMBLE = "ASK { GRAPH <";
	private static final String ASK_POSTAMLE = "> { ?s ?p ?o } }";
	private static final String ASK_RESPONSE_TOKEN = "boolean";
	private static final String EXISTS_ERROR = "Error while trying to run ";
	
	// for method: delete
	private static final String QUERY_CLEAR_GRAPH = "CLEAR GRAPH <";
	private static final String QUERY_CLOSE_URI = ">";
	private static final String QUERY_DELETE_THING_1 = Utils.buildMessage("DELETE WHERE { GRAPH <",MANAGEMENT_GRAPH,"> {  <");
	private static final String QUERY_DELETE_THING_2 =	"> ?p ?o . } }";
	// -- Constructor

	private ThingsService() {
		
	}
	
	// -- Methods
	
	private static final String createGraphId(String id) {
		return Utils.buildMessage(NAMEDGRAPH_PREFIX,id);
	}
	
	protected static final boolean exists(String graphId) {
		String query = Utils.buildMessage(ASK_PREAMBLE, graphId, ASK_POSTAMLE);
		ByteArrayOutputStream baos = Sparql.query(query, ResultsFormat.FMT_RS_JSON);
		String rawResponse = baos.toString();
		JsonObject response = Utils.toJson(rawResponse);
		if (response.has(ASK_RESPONSE_TOKEN)) {
			return response.get(ASK_RESPONSE_TOKEN).getAsBoolean();
		} else {
			throw new RemoteSparqlEndpointException(EXISTS_ERROR+query);
		}
	}
	
	protected static final boolean createUpdateThing(JsonObject td, String id) {
		String graphId = createGraphId(id);
		Boolean exists = exists(graphId);
		updateThing(td, id);
		return exists;
	}
	
	protected static final void updateThing(JsonObject td, String id) {	
		String graphId = createGraphId(id);
		// Prepare management info
		String managementQuery = prepareManagementInformation(td,  graphId);
		// Prepare td
		enrichTd(td);
		Model thing = Things.toModel(td);
		// DONE: Validate td
		Validation.semanticValidation(id, thing);
		Validation.syntacticValidation(td);
		// Store + event
		String query = Utils.buildMessage("\n DROP GRAPH <"+graphId+"> ; INSERT DATA { GRAPH <",graphId,"> { ",Things.printModel(thing, "NT"),"} ",managementQuery," }");
		Sparql.update(query);
		EventsController.eventSystem.igniteEvent(id, DirectoryEvent.CREATE);
	}
	
	protected static final void createThing(JsonObject td, String id) {	
		String graphId = createGraphId(id);
		if(exists(graphId))
			throw new ThingException(Utils.buildMessage(EXCEPTION_MSG_ALREADY_EXISTS));
		// Prepare management info
		String managementQuery = prepareManagementInformation(td,  graphId);
		// Prepare td
		enrichTd(td);
		Model thing = Things.toModel(td);
		// DONE: Validate td
		Validation.semanticValidation(id, thing);
		Validation.syntacticValidation(td);
		// Store + event
		String query = Utils.buildMessage("\nINSERT DATA { GRAPH <",graphId,"> { ",Things.printModel(thing, "NT"),"} ",managementQuery," }");
		Sparql.update(query);
		EventsController.eventSystem.igniteEvent(id, DirectoryEvent.CREATE);
	}
	
	private static String prepareManagementInformation(JsonObject td, String graphId) {
		String security = Utils.buildEncoded(td.get("securityDefinitions").toString());
		Boolean hasTypeThing =  Things.hasThingType(td);
		
		String rawContext = td.get("@context").toString();
		if(Utils.InjectRegistrationInfo)
			rawContext = Things.inject(td, "@context", Things.TDD_RAW_CONTEXT).toString();
	
		String frame = Utils.buildEncoded(" { \"@context\" : ",rawContext , ", \"@type\" : \"Thing\" }");
		if(td.has("@type") && hasTypeThing) {
			frame = Utils.buildEncoded(" { \"@context\" : ",rawContext, ", \"@type\" : ",td.get("@type").toString()," }");
		}else if(td.has("@type") && !hasTypeThing) {
			frame = Utils.buildEncoded(" { \"@context\" : ",rawContext , ", \"@type\" : ",Things.inject(td, "@type", "Thing").toString()," }");
		}
		return Utils.buildMessage("GRAPH <",MANAGEMENT_GRAPH,"> { <",graphId,"> <hive:b64:security> \"",security,"\" . <",graphId,"> <hive:b64:frame> \"",frame,"\" . <",graphId,"> <hive:b64:type> \"",hasTypeThing.toString(),"\"}");
	}
	
	private static void enrichTd(JsonObject td) {
		if(!Things.hasThingType(td))
			td.add("@type", (new Gson()).fromJson( Things.inject(td, "@type", "Thing"), JsonElement.class));
		if(Utils.InjectRegistrationInfo) {
			//TODO:
			JsonArray contexts = new JsonArray();
			if(! (td.get("@context") instanceof JsonArray)) {
				contexts.add(td.get("@context").getAsString());
			}else {
				contexts = td.get("@context").getAsJsonArray();
			}
			if(!jsonArrayContains(contexts,Utils.DISCOVERY_CONTEXT))
				contexts.add(Utils.DISCOVERY_CONTEXT);
			td.add("@context", contexts);
			
			JsonObject registrationInfo = new JsonObject();
			if(!td.has("registration")) {
				registrationInfo.addProperty("created", now());
			}else {
				registrationInfo = td.get("registration").getAsJsonObject();
			}
			registrationInfo.addProperty("modified", now());
			td.add("registration", registrationInfo);
		}
	}
	
	protected static boolean jsonArrayContains(JsonArray elems, String elem) {
		boolean contained=  false;
		for(int index=0; index < elems.size(); index++) {
			contained = elems.get(index).equals(elem);
			if(contained)
				break;
		}
		return contained;
	}
	
	private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
	protected static String now() {
		DateTime date = new DateTime();
		return fmt.print(date);
	}
	
	

	/**
	 * This method finds a Thing
	 * @param id of the Thing to be found
	 * @return returns a JSON-LD 1.1 representation of the Thing
	 */
	public static final JsonObject retrieveThing(String id) {
		String graphId = createGraphId(id);
		if(!exists(graphId))
			throw new NotFoundException(Utils.buildMessage("Requested Thing not found"));
		
		// Retrieve meta information of Thing
		String query = Utils.buildMessage("SELECT ?security ?frame ?type WHERE { GRAPH <",MANAGEMENT_GRAPH, "> { <",graphId,"> <hive:b64:security> ?security ; <hive:b64:frame> ?frame; <hive:b64:type> ?type . } }");
		ByteArrayOutputStream baos = Sparql.query(query, ResultsFormat.FMT_RS_CSV);
		String baosRaw = baos.toString().replace("security,frame,type", "").trim();
		if(baosRaw.isEmpty())
			throw new ThingException(Utils.buildMessage("Requested Thing not found"));
		String[] rawResponse = baosRaw.split(",");
		String security = new String(Base64.getDecoder().decode(rawResponse[0].getBytes()));
		String frame = new String(Base64.getDecoder().decode(rawResponse[1].getBytes()));
		frame = fixFrame(frame);
		// TODO: el context erroneo esta en este frame, hay que ocuparse de el
		Boolean type = Boolean.valueOf(new String(rawResponse[2]));
		// Retrieve Thing
		query = Utils.buildMessage("CONSTRUCT {?s ?p ?o } WHERE { GRAPH <",graphId,"> { ?s ?p ?o .} }");
		baos = Sparql.query(query, ResultsFormat.FMT_RDF_JSONLD);
		
		JsonObject response = Utils.toJson(baos.toString());
		response = Things.toJsonLd11(response, frame);
		response.add("securityDefinitions", Utils.toJson(security));

		if(!type) 
			Things.cleanThingType(response);
		System.out.println(response);
		return response;
	}
	
	
	private static String fixFrame(String frame) {
		return frame.replace("https://w3c.github.io/wot-discovery/context/discovery-context.jsonld","https://www.w3.org/2022/wot/discovery");
	}

	public static List<String> retrieveThingsIds(Integer limit, Integer offset){
		String query = Utils.buildMessage("SELECT DISTINCT ?graph {  GRAPH <",MANAGEMENT_GRAPH,"> {  ?graph ?p ?o .  } } ");
		if(limit!=null)
			query = Utils.buildMessage(query," limit ", limit.toString());
		if(offset!=null)
			query = Utils.buildMessage(query," offset ", offset.toString());
	
		ByteArrayOutputStream baos = Sparql.query(query, ResultsFormat.FMT_RS_CSV);
		String[] ids = baos.toString().split("\n");
		List<String> completeIds = new ArrayList<>();
		for(int index=1; index < ids.length; index++)
			completeIds.add(ids[index].substring(NAMEDGRAPH_PREFIX.length()).trim());
		return completeIds;
	}
	

	public static void updateThingPartially(String id, JsonObject partialUpdate) {
		JsonObject thingJson = retrieveThing(id); // createGraphId(id)
		// TODO: mark modification
		JsonObject newThing = Utils.mergePatch(thingJson, partialUpdate);
		updateThing(newThing, id);

		EventsController.eventSystem.igniteEvent(id, DirectoryEvent.UPDATE);

	}
	
	public static void deleteThing(String id) {
		String graphId = createGraphId(id);
		if(!exists(graphId))
			throw new NotFoundException(EXCEPTION_MSG_NOT_FOUND);
		
		String query = Utils.buildMessage(QUERY_CLEAR_GRAPH,graphId,QUERY_CLOSE_URI);
		Sparql.update(query);
		query = Utils.buildMessage(QUERY_DELETE_THING_1,graphId,QUERY_DELETE_THING_2);
		Sparql.update(query);
		EventsController.eventSystem.igniteEvent(id, DirectoryEvent.DELETE);
	}

	
	
	

	
	
	
}
