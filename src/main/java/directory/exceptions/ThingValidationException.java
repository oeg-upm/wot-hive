package directory.exceptions;

import java.io.StringWriter;
import java.util.UUID;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.RDF;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import directory.Utils;
import spark.ExceptionHandler;
import spark.Request;
import spark.Response;

public class ThingValidationException  extends RuntimeException {

	private static final long serialVersionUID = 588858299068660760L; 
	private String code;
	private JsonObject syntacticReport;
	private Model semanticReport;
	private static JsonObject errorTemplate = new JsonObject();
	private static final String ERRORS_ARRAY_KEY = "validationErrors";
	private static final String INSTANCE_KEY = "instance"; // /errors/uuid
	private static final String DETAIL_KEY = "detail"; // 
	// under detail key
	private static final String SEMANTIC_ERROR_DESCRIPTION = "The input did not pass the SHACL Shapes validation";
	private static final String SYNTACTIC_ERROR_DESCRIPTION = "The input did not pass the JSON Schema validation";

	static {
		errorTemplate = new JsonObject();
		errorTemplate.addProperty("title", "Bad Request");
		errorTemplate.addProperty("status", 400);
	}
	
	public ThingValidationException(ThingValidationException aux) {
		super();
		this.code = aux.getCode();
		if(aux.getSemanticReport()!=null)
			this.semanticReport = aux.getSemanticReport();
		if(aux.getSyntacticReport()!=null)
			this.syntacticReport = aux.getSyntacticReport();
	}
	
	public ThingValidationException(String code) {
		super();
		this.code = code;
	}
	
	public ThingValidationException(String code, JsonObject syntacticReport) {
		super();
		this.code = code;
		this.syntacticReport = syntacticReport;
	}
	
	public ThingValidationException(String code, Model semanticReport) {
		super();
		this.code = code;
		this.semanticReport = semanticReport;
	}

	public String getCode() {
		return code;
	}
	
	public JsonObject getSyntacticReport() {
		return syntacticReport;
	}

	public Model getSemanticReport() {
		return semanticReport;
	}

	// static methods

	public static final String EXCEPTION_CODE_SEM_1 = "validation-semantic-001"; // no shape is present
	public static final String EXCEPTION_CODE_SYN_1 = "validation-syntactic-001"; // no schema is present
	public static final String EXCEPTION_CODE_SEM_2 = "validation-semantic-002"; // shapes report
	public static final String EXCEPTION_CODE_SYN_2 = "validation-syntactic-002"; // schema report
	public static final String EXCEPTION_CODE_SEM_3 = "validation-semantic-003"; // error handling thing, check syntax
	public static final String EXCEPTION_CODE_SYN_3 = "validation-syntactic-003"; // error handling thing, check syntax
	
	public static final ExceptionHandler handleException = (Exception exception, Request request, Response response) -> {
		response.type(Utils.MIME_JSON);
		response.status(400);
		response.header(Utils.HEADER_CONTENT_TYPE, Utils.MIME_DIRECTORY_ERROR);
		ThingValidationException specificException = (ThingValidationException) exception;
		// Set error id and detail
		JsonObject error = errorTemplate.deepCopy();
		setErrorDetails( specificException,  error);
		// Set static error messages
		setStaticErrorMessages(specificException, error);
		// Set dynamic error messages
		if(specificException.getCode().equals(EXCEPTION_CODE_SEM_2)) {
			JsonArray errors = new JsonArray();
			compileErrorSemantic(specificException.getSemanticReport(), errors);
			error.add(ERRORS_ARRAY_KEY, errors);
		}
		if(specificException.getCode().equals(EXCEPTION_CODE_SYN_2)) {
			JsonArray errors = new JsonArray();
			compileErrorSyntactic(specificException.getSyntacticReport(), errors);
			error.add(ERRORS_ARRAY_KEY, errors);
		}

		response.body(error.toString());
	};
	
	private static final void setErrorDetails(ThingValidationException specificException, JsonObject error) {
		error.addProperty(INSTANCE_KEY, Utils.buildMessage("/errors/", UUID.randomUUID().toString()));
		if(specificException.getCode().equals(EXCEPTION_CODE_SEM_1) || specificException.getCode().equals(EXCEPTION_CODE_SEM_2)|| specificException.getCode().equals(EXCEPTION_CODE_SEM_3))
			error.addProperty(DETAIL_KEY, SEMANTIC_ERROR_DESCRIPTION);
		if(specificException.getCode().equals(EXCEPTION_CODE_SYN_1) || specificException.getCode().equals(EXCEPTION_CODE_SYN_2)|| specificException.getCode().equals(EXCEPTION_CODE_SYN_3))
			error.addProperty(DETAIL_KEY, SYNTACTIC_ERROR_DESCRIPTION);
	}
	
	private static final void setStaticErrorMessages(ThingValidationException specificException, JsonObject error) {
		if(specificException.getCode().equals(EXCEPTION_CODE_SEM_1))
			error.add(ERRORS_ARRAY_KEY, compileErrorStatic("SHACL shapes validation is enabled, however no shape was found at ./shape.ttl"));
		if(specificException.getCode().equals(EXCEPTION_CODE_SYN_1))
			error.add(ERRORS_ARRAY_KEY, compileErrorStatic("JSON Schema validation is enabled, however no schema was found at ./schema.json"));
		if(specificException.getCode().equals(EXCEPTION_CODE_SEM_3) || specificException.getCode().equals(EXCEPTION_CODE_SYN_3))
			error.add(ERRORS_ARRAY_KEY, compileErrorStatic("An unexpected error ocurred handling the provided Thing when validating, please review its syntax and/or contact the WoTHive technical team"));
	}
	
	private static final String FIELD_KEY  = "field";
	private static final String DESCRIPTION_KEY  = "description";
	private static final JsonArray compileErrorStatic(String message) {
		JsonArray errorsArray = new JsonArray();
		JsonObject error = new JsonObject();
		error.addProperty(FIELD_KEY, "None");
		error.addProperty(DESCRIPTION_KEY, message);
		errorsArray.add(error);
		return errorsArray;
	}

	private static void compileErrorSyntactic(JsonObject report, JsonArray errorsArray) {
		JsonObject error = new JsonObject();
		error.addProperty(FIELD_KEY, report.get("schemaLocation").getAsString());
		error.addProperty(DESCRIPTION_KEY, report.get("message").getAsString());
		error.addProperty("pointerToViolation", report.get("pointerToViolation").getAsString());
		error.addProperty("keyword", report.get("keyword").getAsString());
		errorsArray.add(error);
		JsonArray nestedErrors = report.get("causingExceptions").getAsJsonArray();
		if(nestedErrors.size()>0) {
			for(int index=0; index < nestedErrors.size(); index++) {
				JsonObject nestedError = nestedErrors.get(index).getAsJsonObject();
				compileErrorSyntactic(nestedError, errorsArray);
			}
		}
	}
	
	
	private static final Resource VALIDATION_RESULT_TYPE = ResourceFactory.createResource("http://www.w3.org/ns/shacl#ValidationResult");
	private static void compileErrorSemantic(Model report, JsonArray errorsArray) {
		ResIterator iterator = report.listSubjectsWithProperty(RDF.type, VALIDATION_RESULT_TYPE);
		while(iterator.hasNext()) {
			JsonObject error = mapSemanticToJson(report, iterator.next());
			errorsArray.add(error);
		}
	}
	
	protected static JsonObject mapSemanticToJson(Model report, Resource subject) {
		JsonObject error = new JsonObject();
		StmtIterator iterator = report.listStatements(subject, null, (RDFNode) null);
		Boolean added = false;
		while(iterator.hasNext()) {
			Statement st = iterator.next();
			
			Node predicate = st.getPredicate().asNode();
			if(predicate.equals(SHACL.resultPath))
				error.addProperty(FIELD_KEY, st.getObject().toString());
			if(predicate.equals(SHACL.resultMessage))
				error.addProperty(DESCRIPTION_KEY, st.getObject().toString());
			if(!added) {
				StringWriter writer = new StringWriter();
				st.getSubject().getModel().write(writer, "TURTLE");
				error.addProperty("sh:result", writer.toString());
				added = true;
			}
				
		}
		
		return error;
		
	}

}
