package directory.things;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFReader;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.FactoryRDF;
import org.apache.jena.riot.system.FactoryRDFStd;

import com.google.gson.JsonObject;

import directory.Directory;
import directory.Utils;
import directory.exceptions.ThingParsingException;
import directory.exceptions.ThingRegistrationException;
import directory.exceptions.ThingValidationException;
import wot.jtd.JTD;
import wot.jtd.Validation;
import wot.jtd.exception.SchemaValidationException;
import wot.jtd.model.Thing;

public class ThingsMapper {

	private static final String ID = "id";

	private ThingsMapper() {
		super();
	}

	protected static String thingToString(Thing thing, RDFFormat format) {
		try {
			if (format.equals(Utils.THING_RDFFormat)) {
				return thing.toJson().toString();
			} else {
				return thingToRDF(thing, format);
			}
		} catch (IOException e) {
			Directory.LOGGER.error(e.toString());
		}
		return "";
	}

	protected static byte[] thingToStringBytes(Thing thing, RDFFormat format) {
		return thingToString(thing, format).getBytes();
	}

	// -- JSON factory
	public static Thing createJsonThing(String td) {
		Thing thing = null;
		try {
			JsonObject thingJson = JTD.parseJson(td);
			if (!thingJson.has(ID))
				throw new ThingRegistrationException(
						"Things under the form application/td+json registered using PUT method must provide a valid 'id', otherwhise se the POST method for Things without 'id'");
			thing = Thing.fromJson(thingJson);
		} catch (Exception e) {
			throw new ThingParsingException(
					Utils.buildMessage("Thing under the form application/td+json has errors, check:", e.toString()));
		}
		return thing;
	}

	protected static Thing createJsonThingAnonymous(String td) {
		String anonUUID = Utils.buildMessage("urn:uuid:", UUID.randomUUID().toString()); // generate anon id
		Thing thing = null;
		try {
			JsonObject thingJson = JTD.parseJson(td);
			if (thingJson.has(ID))
				throw new ThingRegistrationException(
						"Things registered using POST method must lack of an 'id', otherwhise use the PUT method for Things with a valid 'id'");
			thing = Thing.fromJson(thingJson);
			thing.setId(anonUUID);
		} catch (Exception e) {
			throw new ThingParsingException(
					Utils.buildMessage("Thing under the form application/td+json has errors, check:", e.toString()));
		}
		return thing;
	}

	// -- RDF factory
	protected static final Thing createRDFThing(RDFFormat format, String td) {
		return buildThing(format, td);
	}

	protected static final List<Thing> createRDFThings(RDFFormat format, String td) {
		return buildThings(format, td);
	}

	private static final Thing buildThing(RDFFormat format, String td) {
		Thing thing = null;
		try {
			List<Thing> things = buildThings(format, td);
			if (things.size() == 1) {
				thing = things.get(0);
			} else if (things.isEmpty()) {
				throw new ThingParsingException(
						"Zero Things where parsed, check the provided Thing for syntax errors or if its serialisation matches the provided mime type");
			} else {
				throw new ThingParsingException(
						"More than one Thing was provided in the request, please provide only one Thing per request");
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
			reader.read(model, new ByteArrayInputStream(td.getBytes()), Directory.getConfiguration().getService().getDirectoryURIBase());
			things = JTD.fromRDF(model);
		} catch (SchemaValidationException | IllegalArgumentException e) {
			throw new ThingParsingException(e.toString());
		} catch (Exception e) {
			Directory.LOGGER.error(e.toString());
		}

		return things;
	}

	protected static String thingToRDFWithValidation(Thing thing, RDFFormat format) {
		Model model = ModelFactory.createDefaultModel();
		try {
			model = JTD.toRDF(thing);
		} catch (Exception e) {
			throw new ThingParsingException(e.toString());
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		RDFWriter fasterWriter = model.getWriter(format.getLang().getLabel());
		fasterWriter.write(model, output, Directory.getConfiguration().getService().getDirectoryURIBase());

		ThingsMapper.semanticValidation(thing.getId(), model);
		return new String(output.toByteArray());
	}

	protected static String thingToRDF(Thing thing, RDFFormat format) {
		Model model = ModelFactory.createDefaultModel();
		try {
			model = JTD.toRDF(thing);
		} catch (Exception e) {
			throw new ThingParsingException(e.toString());
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		RDFWriter fasterWriter = model.getWriter(format.getLang().getLabel());
		fasterWriter.write(model, output, Directory.getConfiguration().getService().getDirectoryURIBase());

		return new String(output.toByteArray());

	}

	static final FactoryRDF FACTORY = new FactoryRDFStd() {
		@Override
		public Node createURI(String uriStr) {
			if (uriStr.startsWith(Directory.getConfiguration().getService().getDirectoryURIBase()))
				throw new IllegalArgumentException("URI is not absolute: "
						+ uriStr.substring(Directory.getConfiguration().getService().getDirectoryURIBase().length()));
			return super.createURI(uriStr);
		}
	};

	// -- Validation

	private static final Property SH_CONFORMS = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#conforms");

	public static void semanticValidation(String thingId, Model thing) {
		if (Directory.getConfiguration().getValidation().isEnableShaclValidation()) {
			String shape = readFile(Directory.getConfiguration().getValidation().getShapesFile());
			if (shape.isEmpty())
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SEM_1);
			Model report = ModelFactory.createDefaultModel();
			try {
				report = Validation.shaclShapeValidation(ResourceFactory.createResource(thingId), thing, shape);
			} catch (Exception e) {
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SEM_3);
			}
			if (!report.contains(null, SH_CONFORMS, ResourceFactory.createTypedLiteral(true))) 
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SEM_2, report);
		}
	}

	public static void syntacticValidation(Thing thing) {
		if (Directory.getConfiguration().getValidation().isEnableJsonSchemaValidation()) {
			String schema = readFile(Directory.getConfiguration().getValidation().getSchemaFile());
			if (schema.isEmpty())
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SYN_1);
			JsonObject report = new JsonObject();
			try {
				report = Validation.jsonSchemaValidation(thing, schema);
			} catch (Exception e) {
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SYN_3);
			}
			if (report.size() > 0)
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SYN_2, report);
		}
	}

	private static String readFile(String filePath) {
		String content = "";
		try {
			content = new String(Files.readAllBytes(Paths.get(filePath)));
		} catch (IOException e) {
			Directory.LOGGER.error(e.toString());
		}
		return content;
	}

}
