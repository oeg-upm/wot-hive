package things;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.jena.riot.RDFFormat;
import com.google.gson.JsonObject;

import directory.SPARQLEndpoint;
import directory.Utils;
import directory.events.EventsController;
import directory.exceptions.RemoteException;
import wot.jtd.JTD;
import wot.jtd.model.Thing;

public class ThingsDAO  {

	// -- Attributes
	
	private static final String ASK_KEY = "boolean";
	public static List<String> events = new CopyOnWriteArrayList<>();
	// -- Constructor
	
	private ThingsDAO() {
		super();
	}
	
	// -- Methods
	
	// Create
	
	public static Boolean create(Thing thing, String graphId) {
		Boolean correct = false;
		String query = null;
		ThingsMapper.syntacticValidation(thing);
		
		String thingRDF = ThingsMapper.thingToRDFWithValidation(thing, RDFFormat.NTRIPLES);
		query = Utils.buildMessage("\nINSERT DATA { GRAPH <",graphId,"> { \n",thingRDF,"} }");
		byte[] messageResponse = SPARQLEndpoint.sendUpdateQuery(query).toByteArray();
		if (messageResponse.length>0) 
			throw new RemoteException(new String(messageResponse));
		correct = true;
		EventsController.igniteCreateEvent(thing.getId());
		return correct;
	}
	

	
	// -- Read
	
	protected static List<String> getPaginatedGraphs(Integer limit, Integer offset) {
		String graphs = Utils.buildMessage("SELECT ?g WHERE { GRAPH ?g { ?s ?p ?o . } } LIMIT ", limit.toString(), " OFFSET ", offset.toString());
		String rawResults = SPARQLEndpoint.sendQueryString(graphs, Utils.MIME_CSV);
		List<String> graphIds = Arrays.asList(rawResults.split("\n")).stream().map(elem -> Utils.buildMessage("<",elem.trim(),">")).collect(Collectors.toList());
		graphIds.remove(0); // remove CSV header
		return graphIds;
	}
	
	protected static List<Thing> readAll(List<String> graphIds){
		String query = Utils.buildMessage("CONSTRUCT {?s ?p ?o } WHERE { GRAPH ?graph { ?s ?p ?o .} VALUES ?graph {",graphIds.toString().replaceAll("[\\[\\],]*", "")," } }");
		String output = null;
		try {
			output = SPARQLEndpoint.sendQueryString(query, Utils.MIME_TURTLE);
			return ThingsMapper.createRDFThings(RDFFormat.TURTLE, output);
		} catch(Exception e) {
				throw new RemoteException(e.toString());
		}
	}
	
	public static List<Thing> readAll() {
		String query = Utils.buildMessage("CONSTRUCT {?s ?p ?o } WHERE { GRAPH ?graph { ?s ?p ?o .} }");
		String output = SPARQLEndpoint.sendQueryString(query, Utils.MIME_TURTLE);
		return ThingsMapper.createRDFThings(RDFFormat.TURTLE, output);
	}
	
	protected static Thing read(String graphId) {
		String query = Utils.buildMessage("CONSTRUCT {?s ?p ?o } WHERE { GRAPH <",graphId,"> { ?s ?p ?o .} }");
		String output = SPARQLEndpoint.sendQueryString(query, Utils.MIME_TURTLE);
		return ThingsMapper.createRDFThing(RDFFormat.TURTLE, output);
	
	}
	
	protected static boolean exist(String graphId) {
		String query = Utils.buildMessage("ASK { GRAPH <",graphId,"> { ?s ?p ?o } }");
		String result = SPARQLEndpoint.sendQueryString(query, Utils.MIME_JSON);
		if(result==null)
			return false;
		JsonObject output = JTD.parseJson(result);
		return output.get(ASK_KEY).getAsBoolean();
	}
	
	// -- Update
	
	// -- Delete
	
	protected static void delete(String graphId) {
		String query = Utils.buildMessage("DELETE  { ?s ?p ?o } WHERE { GRAPH <",graphId,">  { ?s ?p ?o } }");
		SPARQLEndpoint.sendUpdateQuery(query);
	}

	
	
	
	
	
	
	
	

	
}
