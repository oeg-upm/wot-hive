package directory.things;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.vocabulary.RDF;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;

import com.google.gson.JsonObject;
import org.everit.json.schema.ValidationException;

import directory.Directory;
import directory.Utils;
import directory.exceptions.ThingValidationException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Validation {

	private static final Property SH_CONFORMS = ResourceFactory.createProperty("http://www.w3.org/ns/shacl#conforms");

	public static void semanticValidation(String thingId, Model thing)  {
		
		if (Directory.getConfiguration().getValidation().isEnableShaclValidation()) {
			Model shape  = ModelFactory.createDefaultModel();
			try {
				shape.read(new FileInputStream(new File(Directory.getConfiguration().getValidation().getShapesFile())), Directory.getConfiguration().getService().getDirectoryURIBase(), "TURTLE");
			} catch (FileNotFoundException e1) {
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SEM_1);
			}
			
			if (shape.isEmpty())
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SEM_1);
			Model report = ModelFactory.createDefaultModel();
			try {
				report = shaclShapeValidation(ResourceFactory.createResource(thingId), thing, shape);
			} catch (Exception e) {
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SEM_3);
			}
			if (!report.contains(null, SH_CONFORMS, ResourceFactory.createTypedLiteral(true))) 
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SEM_2, report);
		}
	}

	public static void syntacticValidation(JsonObject thing) {
		if (Directory.getConfiguration().getValidation().isEnableJsonSchemaValidation()) {
			String schema=null;
			try {
				schema = Utils.readFile(new File(Directory.getConfiguration().getValidation().getSchemaFile()));
			} catch (IOException e1) {
				throw new ThingValidationException(ThingValidationException.EXCEPTION_CODE_SYN_1);

			}
			if (schema!=null && schema.isEmpty())
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

	/**
	 * This method validates a Thing using a JSON Schema. The validation is provided under the form of a {@link JsonObject}, which will be empty if the thing has no errors.<br>
	 * The validation has been implemented with the library <a href="https://github.com/everit-org/json-schema">everit-org json-schema</a>, check their GitHub for more details.
	 * @param thing the {@link Thing} to be validated under its {@link JsonObject} form
	 * @param schema a schema to be applied to the {@link Thing}
	 * @return a {@link JsonObject} containing the validation report, this {@link JsonObject} will be empty if the provided {@link Thing} had no errors.
	 */
	public static JsonObject jsonSchemaValidation(JsonObject thing, String schema){
		JsonObject report = new JsonObject();
		try {
			JSONObject jsonObject = new JSONObject(new JSONTokener(new ByteArrayInputStream(thing.toString().getBytes())));
			JSONObject rawSchema = new JSONObject(new JSONTokener(new ByteArrayInputStream(schema.getBytes())));
			Schema schemaObj = SchemaLoader.load(rawSchema);
			schemaObj.validate(jsonObject);
		} catch (ValidationException e) {
			report = Utils.toJson(e.toJSON().toString());
		}
		return report;
	}
	
	private static final Resource THING_TYPE = ResourceFactory.createResource("https://www.w3.org/2019/wot/td#Thing");
	/**
	 * This method validates a Thing using <a href="https://www.w3.org/TR/shacl/">W3C SHACL shapes</a>. The validation is provided under the form of a {@link Model}.<br>
	 * @param thingId the resource identifying the {@link Thing}
	 * @param thing the {@link Thing} to be validated under its {@link Model} form (RDF)
	 * @param shape a SHACL shape to be applied to the {@link Thing}
	 * @return a {@link Model} containing the validation report
	 */
	public static Model shaclShapeValidation(Resource thingId, Model thing, Model shape) {
		Shapes shapes = Shapes.parse(shape.getGraph());
		Model modelAux = ModelFactory.createDefaultModel();
		modelAux.add(thing);
		modelAux.add(thingId, RDF.type, THING_TYPE);
		ValidationReport validationReport = ShaclValidator.get().validate(shapes, modelAux.getGraph());
		return validationReport.getModel();
	}
}
