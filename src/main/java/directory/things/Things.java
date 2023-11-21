package directory.things;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.JsonLdVersion;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.rdf.RdfDataset;
import com.apicatalog.rdf.RdfLiteral;
import com.apicatalog.rdf.RdfNQuad;
import com.apicatalog.rdf.RdfValue;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import directory.Directory;
import directory.Utils;
import directory.exceptions.ThingException;

public class Things {
	
	
	public static String TDD_RAW_CONTEXT = "https://w3c.github.io/wot-discovery/context/discovery-core.jsonld";
	
	protected static String inject(JsonObject json, String key, String injection) {
		JsonElement value = json.deepCopy().get(key);
		JsonArray values = new JsonArray();
		if(value instanceof JsonArray) {
			values = value.getAsJsonArray();
		}else if(value instanceof JsonPrimitive) {
			values.add(value);
		}
		values.add(injection);
		return values.toString();
	}
	
	protected static void cleanThingType(JsonObject td) {
		if(td.has("@type")) {
			if( td.get("@type") instanceof JsonPrimitive) {
				td.remove("@type");
			}else if(td.get("@type") instanceof JsonArray) {
				JsonArray array = td.remove("@type").getAsJsonArray();
				array.remove(new JsonPrimitive("Thing"));	
				array.remove(new JsonPrimitive("https://www.w3.org/2019/wot/td#Thing"));				
				td.add("@type", array);
			}
		}
	}
	
	protected static Boolean hasThingType(JsonObject td) {
		boolean hasType = td.has("@type");
		if(hasType) {
			JsonElement type = td.get("@type");
			hasType = type instanceof JsonPrimitive && (type.toString().equals("Thing") || type.toString().equals("https://www.w3.org/2019/wot/td#Thing"));
			if(!hasType && type instanceof JsonArray) {
				JsonArray array = type.getAsJsonArray();
				hasType = array.contains(new JsonPrimitive("Thing")) || array.contains(new JsonPrimitive("https://www.w3.org/2019/wot/td#Thing"));
			}
		}
		return hasType;
	}
	
	
	
	public static JsonObject toJsonLd11(JsonObject jsonLd, String frame) {
		try {
			Document document = JsonDocument.of(new StringReader(jsonLd.toString()));
			Document frameDocument = JsonDocument.of(new StringReader(frame));
			JsonLdOptions options = new JsonLdOptions();
			options.setBase(new URI(Directory.getConfiguration().getService().getDirectoryURIBase()));
			options.setCompactArrays(true); // // IMPORTANT FOR WOT TEST SUITE
			
			options.setCompactToRelative(true);
			options.setExplicit(false);
			options.setProduceGeneralizedRdf(true);
			options.setProcessingMode(JsonLdVersion.V1_1);
			String thing = JsonLd.frame(document, frameDocument).options(options).get().toString();
			JsonObject thingJson = Utils.toJson(thing.replaceAll(Directory.getConfiguration().getService().getDirectoryURIBase(), ""));
			// TODO: needed for wot validation
			if(thingJson.has("hasSecurityConfiguration")) {
				JsonElement elem = thingJson.remove("hasSecurityConfiguration");
				thingJson.add("security", elem);
			}
			if(thingJson.has("security") && !(thingJson.get("security") instanceof JsonArray) ) {
				JsonArray sec = new JsonArray();
				sec.add(thingJson.remove("security"));
				thingJson.add("security", sec);
			}
			return thingJson;
		}catch(Exception e) {
			throw new ThingException("Error translating JSON-LD 1.0 into JSON-LD 1.1");
		}
	}
	
	
	public static String printModel(Model model, String serialisation) {
		Writer writer = new StringWriter();
		model.write(writer, serialisation, Directory.getConfiguration().getService().getDirectoryURIBase());
		return writer.toString();
	}
	
	public static Model toModel(JsonObject td) {
		Model model = ModelFactory.createDefaultModel();
		toRDF(td).toList()
			.stream()
				.forEach(elem -> model.add(toTriple(elem)));
		return model;
	}
	
	private static RdfDataset toRDF(JsonObject jsonld11) {
		try {
			Document jsonDocument = JsonDocument.of(new StringReader(jsonld11.toString()));
			JsonLdOptions options = new JsonLdOptions();
			options.setBase(new URI(Directory.getConfiguration().getService().getDirectoryURIBase()));
			options.setProcessingMode(JsonLdVersion.V1_1);
			return JsonLd.toRdf(jsonDocument).options(options).get();
		} catch (JsonLdError | URISyntaxException e) {
			throw new ThingException("Error translating JSON-LD 1.1 into RDF");
		}
	}
	

	private static Model toTriple(RdfNQuad quadTriple) {
		Model model = ModelFactory.createDefaultModel();
		
		try {
		Resource subject = ResourceFactory.createResource(quadTriple.getSubject().toString());
		Property predicate =  ResourceFactory.createProperty(quadTriple.getPredicate().toString());
		RdfValue objectRaw = quadTriple.getObject();
		
		if(objectRaw.isIRI() || objectRaw.isBlankNode()) {
			Resource object =  ResourceFactory.createResource(quadTriple.getObject().getValue());
			model.add(subject, predicate, (RDFNode) object);
		}else {
			RdfLiteral literal = objectRaw.asLiteral();
			Literal jenaLiteral = ResourceFactory.createPlainLiteral(literal.getValue());
			if(literal.getLanguage().isPresent()) {
				jenaLiteral = ResourceFactory.createLangLiteral(literal.getValue(), literal.getLanguage().get());
			}else if(literal.getDatatype()!=null && !literal.getDatatype().isEmpty()) {
				jenaLiteral = ResourceFactory.createTypedLiteral(literal.getValue(), new BaseDatatype(literal.getDatatype()));
			}
			model.add(subject, predicate, jenaLiteral);
		}
		}catch(Exception e) {
			Directory.LOGGER.error("Error adding tirple : "+quadTriple);
			Directory.LOGGER.error(e.toString());
		}
		
		return model;
	}


	
	
}
