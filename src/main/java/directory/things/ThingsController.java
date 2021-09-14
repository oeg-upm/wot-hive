package directory.things;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.riot.RDFFormat;

import com.google.gson.JsonObject;

import directory.Utils;
import directory.exceptions.ThingNotFoundException;
import directory.exceptions.ThingRegistrationException;
import spark.Request;
import spark.Response;
import spark.Route;
import wot.jtd.JTD;
import wot.jtd.model.Thing;

public class ThingsController {

	// -- Attributes
	private static final String LOCATION_HEADER = "Location";
	private static String eTag = "ev1";
	
	// -- Constructor
	private ThingsController() {
		super();
	}
	
	// -- Methods
	// TODO: add sort_by & sort_order
	public static final Route listing = (Request request, Response response) -> {
		RDFFormat format = hasValidMime(request.headers(Utils.HEADER_ACCEPT), false);
		Integer limit = request.queryMap("limit").integerValue();
		Integer offset = initoffset(request.queryMap("offset").integerValue());
		// Prepare response content
		String contentFormat = Utils.retrieveMimeFromRDFFormat(format).get();
		if(contentFormat == Utils.MIME_THING)
			response.header(Utils.HEADER_CONTENT_TYPE, "application/ld+json");
		response.status(200);
		// Listing without pagination
		if(limit==null) 
			return ThingsDAO.readAll().stream().map(thing -> ThingsMapper.thingToString(thing, format)).collect(Collectors.toList());
		// Listing with pagination
		List<String> thingsIds = ThingsDAO.getPaginatedGraphs(limit, offset);
		prepareListingResponse( response,  limit,  offset,  thingsIds.size());
		return ThingsDAO.readAll(thingsIds).stream().map(thing -> ThingsMapper.thingToString(thing, format)).collect(Collectors.toList());
	};
	
	private static Integer initoffset(Integer offset) {
		if(offset==null)
			offset = 0;
		return offset;
	}
	
	private static final void prepareListingResponse(Response response, Integer limit, Integer offset, Integer thingsSize) {
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
		String graphId = request.params(":id");
		RDFFormat format = hasValidMime(request.headers(Utils.HEADER_ACCEPT), false);

		Thing thing = ThingsService.retrieveThing(graphId);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.retrieveMimeFromRDFFormat(format).get());
		response.status(200);
		if (format.equals(Utils.THING_RDFFormat)) {
			return thing.toJson().toString().getBytes();
		} else {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			JTD.toRDF(thing).write(output, format.getLang().getName());
			return output;
		}
		
	};
	

	
	public static final Route registrationUpdate = (Request request, Response response) -> {
		String graphId = request.params(":id");
		
		String td = hasValidBody(request.body());
		RDFFormat format = hasValidMime(request.headers(Utils.HEADER_CONTENT_TYPE), true);
		response.status(201);
		if(ThingsDAO.exist(graphId)) // Update
			response.status(204);
		if(format.equals(RDFFormat.JSONLD_FRAME_FLAT)) { // Create
			ThingsService.registerJsonThing(graphId, td);
		}else {
			ThingsService.registerRDFThing(graphId, format, td);
		}
		return "";
	};
	
	public static final Route registrationAnonymous = (Request request, Response response) -> {
		String td = hasValidBody(request.body());
		RDFFormat format = hasValidMime(request.headers(Utils.HEADER_CONTENT_TYPE), true);

		if(format.equals(RDFFormat.JSONLD_FRAME_FLAT)) {
			String newUUID = ThingsService.registerJsonThingAnonymous(td);
			response.header(LOCATION_HEADER, newUUID);
		}else {
			throw new ThingRegistrationException("Things under a different form than application/td+json must be registered using PUT");
		}
		response.status(201);
		return "";
	};
	
	
	public static final Route partialUpdate = (Request request, Response response) -> {
		JsonObject tdJson = null;
		String graphId = request.params(":id");
		String td = hasValidBody(request.body());
		if(!request.headers(Utils.HEADER_CONTENT_TYPE).equals("application/merge-patch+json")) 
			throw new ThingRegistrationException("Partial updates require header 'Content-Type' with value 'application/merge-patch+json'");
		try {
			tdJson = JTD.parseJson(td);	
		}catch(Exception e) {
			throw new ThingRegistrationException("Partial updates are only supported for Things under the form of application/td+json, provided update document has syntax errors");
		}
		ThingsService.updateThingPartially(graphId,  tdJson);
		response.status(204);
		
		return "";
	};
	
	// -- Deletion
	
	// DELETE delete TD
	public static final Route deletion = (Request request, Response response) -> {
			try{
				String graphId = request.params(":id");
				if(graphId!=null && ThingsDAO.exist(graphId)) {
					ThingsDAO.delete(graphId);
					response.status(204);
				}else {
					throw new ThingNotFoundException(Utils.buildMessage("Requested Thing not found"));
				}
			}catch(Exception e) {
				throw new ThingNotFoundException(Utils.buildMessage("Requested Thing not found"));
			}
			return "";
		};
		
	// -- Ancillary methods
	
	/**
	 * This method checks if the provided request has a valid body, i.e., not empty nor null
	 * @param body the body sent in the request
	 * @return if the body is valid it returns the body's value, otherwise it returns null
	 */
	private static String hasValidBody(String body) {
		Boolean isValid = body!=null && !body.isEmpty();
		if(!isValid)
			throw new ThingRegistrationException("Provided body in request was empty. Provide the TD to be registered in the body of the request");
		return body;
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
			throw new ThingRegistrationException(Utils.buildMessage("Provided mime type is not supported. Provide one mime type from the available ", Utils.WOT_TD_MYMES.keySet().toString()));
		}else if(!strict && tdFormat==null) {
			tdFormat = RDFFormat.JSONLD_FRAME_FLAT;
		}
		return tdFormat;
	}
	

		
	
}
