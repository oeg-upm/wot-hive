package directory.things;

import java.io.IOException;
import org.apache.jena.riot.RDFFormat;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.github.fge.jsonpatch.JsonPatchException;
import com.google.gson.JsonObject;

import directory.Utils;
import directory.exceptions.RemoteException;
import directory.exceptions.ThingNotFoundException;
import directory.exceptions.ThingParsingException;
import directory.exceptions.ThingRegistrationException;
import wot.jtd.JTD;
import wot.jtd.model.Thing;
import wot.jtd.model.directory.RegistrationInformation;

public class ThingsService {

	// -- Attributes

	
	// -- Constructor

	private ThingsService() {
		
	}

	
	// -- Methods
	
	protected static final Thing retrieveThing(String graphId) {
		if(!ThingsDAO.exist(graphId))
			throw new ThingNotFoundException(Utils.buildMessage("Requested Thing not found"));
		return ThingsDAO.read(graphId);
	}
	
	public static final String registerJsonThingAnonymous(String td) {
		Thing thing = ThingsMapper.createJsonThingAnonymous(td);
		enrichTD(thing);
		ThingsDAO.create(thing, thing.getId());
		return thing.getId();
	}
	
	protected static final Boolean registerJsonThing(String graphId, String td) {
		Boolean registered = true;
		Thing thing = ThingsMapper.createJsonThing(td);
		if(ThingsDAO.exist(graphId)) {
			ThingsDAO.delete(graphId);
			markModification(thing);
		}else {
			enrichTD(thing);
		}
		ThingsDAO.create(thing, graphId);

		return registered;
	}

	protected static final Boolean registerRDFThing(String graphId, RDFFormat format, String td) {
		Boolean registered = null;
		try {
			Thing thing = ThingsMapper.createRDFThing(format, td);
			if(ThingsDAO.exist(graphId)) {
				ThingsDAO.delete(graphId);
				markModification(thing);
			}else {
				enrichTD(thing);
			}
			ThingsDAO.create(thing, graphId);
		}catch(RemoteException e) {
			throw new RemoteException(e.toString());
		}
		return registered;
	}
	

	public static void updateThingPartially(String graphId, JsonObject partialUpdate) {
		Thing existingThing = ThingsService.retrieveThing(graphId);
		try {
			Thing updatedThing = JTD.updateJsonThingPartially(existingThing, partialUpdate);
			markModification(updatedThing);
			ThingsService.registerJsonThing(graphId, updatedThing.toJson().toString());
		} catch (IOException e) {
			throw new ThingParsingException(e.toString());
		} catch (JsonPatchException e) {
			throw new ThingRegistrationException(e.toString());
		}
	}

	private static void markModification(Thing thing) {
		RegistrationInformation registrationInformation = new RegistrationInformation();
		String now = now();
		registrationInformation.setModified(now);
		thing.setRegistrationInformation(registrationInformation);

	}
	
	private static void enrichTD(Thing thing) {
		if(thing.getRegistrationInformation()==null) {
			RegistrationInformation registrationInformation = new RegistrationInformation();
			String now = now();
			registrationInformation.setCreated(now);
			registrationInformation.setModified(now);
			thing.setRegistrationInformation(registrationInformation);
		}else {
			
		}

	}

	
	private static final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
	private static String now() {
		DateTime date = new DateTime();
		return fmt.print(date);
	}
	

	
	
	
}
