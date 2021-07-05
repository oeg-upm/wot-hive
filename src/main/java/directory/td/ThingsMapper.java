package directory.td;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFReader;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.FactoryRDF;
import org.apache.jena.riot.system.FactoryRDFStd;

import com.google.gson.JsonObject;

import directory.Directory;
import directory.Utils;
import directory.exceptions.ThingParsingException;
import directory.exceptions.ThingRegistrationException;
import wot.jtd.JTD;
import wot.jtd.exception.SchemaValidationException;
import wot.jtd.model.Thing;

public class ThingsMapper {

	private static final String ID = "id";
	
	private ThingsMapper() {
		super();
	}

	
	protected static String thingToString(Thing thing, RDFFormat format) {
		try {
			if(format.equals(Utils.THING_RDFFormat)) {
				return thing.toJson().toString();
			}else {
				return thingToRDF(thing, format);
			}
		}catch(IOException | IllegalAccessException | ClassNotFoundException | URISyntaxException | SchemaValidationException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	protected static byte[] thingToStringBytes(Thing thing, RDFFormat format) {
		return thingToString( thing,  format).getBytes();
	}
	
	// -- JSON factory
	public static Thing createJsonThing(String td) {
		Thing thing = null;
		try {
			JsonObject thingJson =JTD.parseJson(td);
			if(!thingJson.has(ID)) 
				throw new ThingRegistrationException("Things under the form application/td+json registered using PUT method must provide a valid 'id', otherwhise se the POST method for Things without 'id'");
			thing = Thing.fromJson(thingJson);
		}catch(Exception e) {
			throw new ThingParsingException(Utils.buildMessage("Thing under the form application/td+json has errors, check:", e.toString()));
		}
		return thing;
	}
	
	
	protected static Thing createJsonThingAnonymous(String td) {
		String anonUUID = Utils.buildMessage("urn:uuid:",UUID.randomUUID().toString()); // generate anon id
		Thing thing = null;
		try {
			JsonObject thingJson =JTD.parseJson(td);
			if(thingJson.has(ID)) 
				throw new ThingRegistrationException("Things registered using POST method must lack of an 'id', otherwhise use the PUT method for Things with a valid 'id'");
			thing = Thing.fromJson(thingJson);
			thing.setId(anonUUID);
		}catch(Exception e) {
			throw new ThingParsingException(Utils.buildMessage("Thing under the form application/td+json has errors, check:", e.toString()));
		}
		return thing;
	}
	
	
	// -- RDF factory
	protected static final Thing createRDFThing(RDFFormat format, String td) {
		return buildThing( format,  td);
	}
	
	protected static final List<Thing> createRDFThings(RDFFormat format, String td) {
		return buildThings( format,  td);
	}
	
	private static final Thing buildThing(RDFFormat format, String td) {
		Thing thing = null;
		try {
			List<Thing> things = buildThings(format, td);
			if(things.size()==1) {
				thing = things.get(0);
			}else if(things.isEmpty()) {
				throw new ThingParsingException("Zero Things where parsed, check the provided Thing for syntax errors or if its serialisation matches the provided mime type");
			}else {
				throw new ThingParsingException("More than one Thing was provided in the request, please provide only one Thing per request");
			}
		} catch (IllegalArgumentException e) {
			throw new ThingParsingException(e.toString());
		}
		return thing;
	}
	
	private static final List<Thing> buildThings(RDFFormat format, String td) {
		List<Thing> things = new ArrayList<>();
		try {
			Model model = ModelFactory.createDefaultModel();
			RDFReader reader = model.getReader(format.getLang().getName().toLowerCase());
			reader.read(model, new ByteArrayInputStream(td.getBytes()), Directory.DIRECTORY_BASE);
			// TODO: VALIDATE MODEL WITH SHAPES AT LEAST
			things = JTD.fromRDF(model);
		} catch (SchemaValidationException | IllegalArgumentException e) {
			throw new ThingParsingException(e.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return things;
	}
	
	protected static String thingToRDF(Thing thing, RDFFormat format) throws IOException, IllegalAccessException, ClassNotFoundException, URISyntaxException, SchemaValidationException {
		Model model = JTD.toRDF(thing);

		ByteArrayOutputStream output = new  ByteArrayOutputStream();
		RDFWriter fasterWriter = model.getWriter(format.getLang().getLabel());
		fasterWriter.write(model, output, Directory.DIRECTORY_BASE);
//		
//		Model newModel = ModelFactory.createDefaultModel();
//		RDFReader reader = newModel.getReader(format.getLang().getName().toLowerCase());
//		reader.read(newModel, new ByteArrayInputStream(td.getBytes()), Directory.DIRECTORY_BASE);
//		
//		
		
		return new String(output.toByteArray());
	}
	


	static final FactoryRDF FACTORY = new FactoryRDFStd() {
	    @Override
	    public Node createURI(String uriStr) {
	        if (uriStr.startsWith(Directory.DIRECTORY_BASE))
	            throw new IllegalArgumentException("URI is not absolute: " + uriStr.substring(Directory.DIRECTORY_BASE.length()));
	        return super.createURI(uriStr);
	    }
	};
	
	
	/**
	 * static final String BASE = "null://null/";
static final FactoryRDF FACTORY = new FactoryRDFStd() {
    @Override
    public Node createURI(String uriStr) {
        if (uriStr.startsWith(BASE))
            throw new IllegalArgumentException("URI is not absolute: " + uriStr.substring(BASE.length()));
        return super.createURI(uriStr);
    }
};

public static void main(String... args) {
    Model output = ModelFactory.createDefaultModel();
    RDFParser.create()
             .fromString("<foo> <bar> <baz> .")
             .lang(Lang.TURTLE)
             .base(BASE)
             .factory(FACTORY)
             .parse(output.getGraph());
}
	 */
	
	
	
}
