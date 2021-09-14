package directory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.jena.riot.RDFFormat;


public class Utils {

	// -- Attributes
	public static final Map<String,RDFFormat> WOT_TD_MYMES = new HashMap<>();
	public static final String MIME_CSV = "text/csv";
	public static final String MIME_JSON = "application/json";
	public static final String MIME_THING = "application/td+json";
	public static final RDFFormat THING_RDFFormat = RDFFormat.JSONLD_FRAME_FLAT;
	public static final String MIME_TURTLE = "text/turtle";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_LINK = "Link";
	public static final String HEADER_ACCEPT = "Accept"; 
	public static final String METHOD_GET = "GET";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_PUT = "PUT";
	
	public static final String MIME_DIRECTORY_ERROR = "application/problem+json";
	protected static final String DIRECTORY_VERSION = "WoTHive/0.1.0";
	protected static final String WOT_DIRECTORY_LOGO = "\n"+
			"██╗    ██╗ ██████╗ ████████╗\n" + 
			"██║    ██║██╔═══██╗╚══██╔══╝\n" + 
			"██║ █╗ ██║██║   ██║   ██║   \n" + 
			"██║███╗██║██║   ██║   ██║   \n" + 
			"╚███╔███╔╝╚██████╔╝   ██║   \n" + 
			" ╚══╝╚══╝  ╚═════╝    ╚═╝   \n" + 
			"                            \n" + 
			"██╗  ██╗██╗██╗   ██╗███████╗\n" + 
			"██║  ██║██║██║   ██║██╔════╝\n" + 
			"███████║██║██║   ██║█████╗  \n" + 
			"██╔══██║██║╚██╗ ██╔╝██╔══╝  \n" + 
			"██║  ██║██║ ╚████╔╝ ███████╗\n" + 
			"╚═╝  ╚═╝╚═╝  ╚═══╝  ╚══════╝\n" + 
			"\t© Ontology Engineering Group at Universidad Politectnica de Madrid\n"+
			"\tAuthor: Andrea Cimmino\n";

	
	
	static {
		WOT_TD_MYMES.put(Utils.MIME_THING, THING_RDFFormat);
		WOT_TD_MYMES.put(Utils.MIME_TURTLE, RDFFormat.TURTLE);
		WOT_TD_MYMES.put("application/x-turtle", RDFFormat.TURTLE);
		WOT_TD_MYMES.put("application/n-triples", RDFFormat.NTRIPLES);
	}
	
	// -- Constructor
	private Utils() {
		super();
	}
	
	// -- Methods
	
	public static Optional<String> retrieveMimeFromRDFFormat(RDFFormat format) {
		return WOT_TD_MYMES.entrySet().stream().filter(elem -> elem.getValue().equals(format)).findFirst().map(Entry::getKey);
	}
	
	public static String createErrorMessage(String code, String message, String detail) {
		StringBuilder errorMessage = new StringBuilder();
		errorMessage.append("{\n\t\"code\": \"").append(code).append("\",");
		errorMessage.append(" \n\t\"description\": \"").append(message).append("\"");
		if(detail!=null) {
			errorMessage.append(",\n\t\"detail\": \"").append(detail).append("\"");
		}
		errorMessage.append("\n}");
		
		return errorMessage.toString();
	}
	
	public static String buildMessage(String ... values) {
		StringBuilder message = new StringBuilder();
		for(int index=0; index < values.length; index++) {
			message.append(values[index]);
		}
		return message.toString();
	}
	
	private static final String SERVICE_PORT= "--service.port=";
	protected static Integer parseArgumentPort(String[] args, int defaultPort) {
		int port = defaultPort;
		try {
			if(args!=null && args.length>0) {
				String newPort = args[0];
				if(newPort.startsWith(SERVICE_PORT))
					port = Integer.parseInt(newPort.substring(0, SERVICE_PORT.length()));
			}
		}catch(Exception e) {
			throw new IllegalArgumentException("Port for running the service must be provided as argument using the flag '--service.port='");
		}
		return port;	
	}
	
	
	
}
