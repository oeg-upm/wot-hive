package directory.things;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.FramingApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import directory.Utils;
import directory.exceptions.ThingException;
import directory.triplestore.Sparql;
import spark.Request;
import spark.Response;
import spark.Route;

public class ThingsController {

	// -- Attributes
	private static final String LOCATION_HEADER = "Location";
	private static String eTag = "ev1";
	private static final String THING_TOKEN_ID1 = "@id";
	private static final String THING_TOKEN_ID2 = "id";
	// -- Constructor
	private ThingsController() {
		super();
	}
	
	// -- Methods
	// TODO: add sort_by & sort_order
	public static final Route listing = (Request request, Response response) -> {
		RDFFormat format = hasValidMime(request.headers(Utils.HEADER_ACCEPT), false);
		if(!format.equals(RDFFormat.JSONLD_FRAME_FLAT)) 
			throw new ThingException("Things under a different form than application/td+json are not supported yet; create an issue for requesting this functionality");

		Integer limit = request.queryMap("limit").integerValue();
		Integer offset = request.queryMap("offset").integerValue();

		response.header(Utils.HEADER_CONTENT_TYPE, "application/ld+json");
		response.status(200);
		
		List<String> thingsIds = ThingsService.retrieveThingsIds(limit, offset);
		if(limit!=null) // Listing with pagination 
			prepareListingResponse(response,  limit,  offset,  thingsIds.size());
		return thingsIds.parallelStream().map(ThingsService::retrieveThing).collect(Collectors.toList());
	};
	
	
	private static final void prepareListingResponse(Response response, Integer limit, Integer offset, Integer thingsSize) {
		if(offset==null)
			offset = 0;
		if(limit!=null) {
			if(thingsSize== limit) {
				if(offset==null || offset == 0) {
					offset = limit;
				}else {
					offset += limit;
				}
				response.header(Utils.HEADER_LINK, Utils.buildMessage("</things?offset=",offset.toString(),"&limit=",limit.toString(),">; rel=\"next\""));
			}
			response.header(Utils.HEADER_LINK, Utils.buildMessage("</things>; rel=\"canonical\"; etag=\"",eTag,"\""));
		}
	}
	
	
	public static final Route retrieval = (Request request, Response response) -> {
		String id = hasValidId(request); 
		RDFFormat format = hasValidMime(request.headers(Utils.HEADER_ACCEPT), false);	
		
		if(!format.equals(RDFFormat.JSONLD_FRAME_FLAT)) 
			throw new ThingException("Things under a different form than application/td+json sent to be registered are not supported yet; create an issue for requesting this functionality");

		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_THING);
		response.status(200);
		return ThingsService.retrieveThing(id);
		
	};
	

	
	public static final Route registrationUpdate = (Request request, Response response) -> {
		String id = hasValidId(request);
		JsonObject td = hasValidBody(request.body());
		RDFFormat format = hasValidMime(request.headers(Utils.HEADER_CONTENT_TYPE), false);
		Boolean exist = false;
		checkIdsConsistency( td,  id);
		if(format.equals(RDFFormat.JSONLD_FRAME_FLAT)) { // Create/Update
			exist = ThingsService.createUpdateThing( td,  id);
		}else {
			throw new ThingException("Things under a different form than application/td+json sent to be registered are not supported yet; create an issue for requesting this functionality");
		}
		
		response.status(201);
		if(exist) // Update
			response.status(204);
		return "";
	};
		
	public static final Route registrationAnonymous = (Request request, Response response) -> {
		RDFFormat format = hasValidMime(request.headers(Utils.HEADER_CONTENT_TYPE), false);
		String tdId = Utils.buildMessage("directory:anon:",UUID.randomUUID().toString());
			
		if(format.equals(RDFFormat.JSONLD_FRAME_FLAT)) {
			JsonObject thing = hasValidBody(request.body());
			if(thing.has(THING_TOKEN_ID1) || thing.has(THING_TOKEN_ID2))
				throw new ThingException("Thing provider has an 'id', please provide a Thing without 'id' or '@id'");
			thing.addProperty(THING_TOKEN_ID1, tdId);
			ThingsService.createThing(thing, tdId);
			response.header(LOCATION_HEADER, tdId);
		}else {
			throw new ThingException("Things under a different form than application/td+json sent to be registered are not supported yet; create an issue for requesting this functionality");
		}
		response.status(201);
		return "";
	};
	
	
	public static final Route partialUpdate = (Request request, Response response) -> {
		if(!request.headers(Utils.HEADER_CONTENT_TYPE).equals("application/merge-patch+json")) 
			throw new ThingException("Partial updates require header 'Content-Type' with value 'application/merge-patch+json'");
		String id = hasValidId(request);
		JsonObject td = hasValidBody(request.body());
		checkIdsConsistency( td,  id);
		ThingsService.updateThingPartially(id, td);
		response.status(204);
		
		return "";
	};
	
	// -- Deletion
	
	// DELETE delete TD
	public static final Route deletion = (Request request, Response response) -> {
		String id = hasValidId(request);
		ThingsService.deleteThing(id);
		response.status(204);
		return "";
	};
		
	// -- Ancillary methods
	
	private static String hasValidId(Request request) {
		String id = request.params(THING_TOKEN_ID2);
		if(id==null)
			throw new ThingException("Please provide a valid Thing id for deleting");
		return id;
	}
	
	/**
	 * This method checks if the provided request has a valid body, i.e., not empty nor null
	 * @param body the body sent in the request
	 * @return if the body is valid it returns the body's value, otherwise it returns null
	 */
	private static JsonObject hasValidBody(String body) {
		Boolean isValid = body!=null && !body.isEmpty();
		if(!isValid)
			throw new ThingException("Provided body in request was empty. Provide the TD to be registered in the body of the request");
		return Utils.toJson(body);
	}
	
	/**
	 * This method checks if the provided mime type is supported by the directory
	 * @param mime a valid <a href="https://datatracker.ietf.org/doc/html/rfc2045">mime type</a>
	 * @param strict 
	 * @return if the mime type is supported it returns an equivalent {@link RDFFormat} for the mime type, otherwise it returns null
	 */
	private static final RDFFormat hasValidMime(String mime, boolean strict) {
		RDFFormat tdFormat = Utils.WOT_TD_MYMES.get(mime);
		if(strict && tdFormat==null) {
			throw new ThingException(Utils.buildMessage("Provided mime type is not supported. Provide one mime type from the available ", Utils.WOT_TD_MYMES.keySet().toString()));
		}else if(!strict && tdFormat==null) {
			tdFormat = RDFFormat.JSONLD_FRAME_FLAT;
		}
		return tdFormat;
	}
		
	private static void checkIdsConsistency(JsonObject td, String id) {
		if(!td.has(THING_TOKEN_ID2) && !td.has(THING_TOKEN_ID1)) {
			throw new ThingException("Please specify the id of the Thing with 'id' or '@id'");
		}else {
			if((td.has(THING_TOKEN_ID2) && !td.get(THING_TOKEN_ID2).getAsString().equals(id)) || (td.has(THING_TOKEN_ID1) && !td.get(THING_TOKEN_ID1).getAsString().equals(id)))
				throw new ThingException("Thing id is not the same than the one provided as argument in the request");	
		}
		
	}
	
}
