package directory.things.store;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import directory.Directory;
import directory.Utils;
import directory.exceptions.RemoteException;

public class SPARQLEndpoint {
	
	// -- Attributes
	private static URI updateEnpoint = null;
	private static URI queryEnpoint = null;
	private static Boolean queryUsingGET = true;
	
	// -- Constructor
	private SPARQLEndpoint() {
		super();
	}
	
	// -- Getters & Setters
	public static URI getUpdateSparqlEndpoint() {
		return updateEnpoint;
	}

	public static URI getQuerySparqlEndpoint() {
		return queryEnpoint;
	}

	
	public static Boolean getQueryUsingGET() {
		return queryUsingGET;
	}

	public static void setQueryUsingGET(Boolean queryUsingGET) {
		SPARQLEndpoint.queryUsingGET = queryUsingGET;
	}

	public static void setUpdateSparqlEndpoint(String updateEndpoint) {
		try {
			if(updateEndpoint!=null)
				SPARQLEndpoint.updateEnpoint = new URI(updateEndpoint);				
		} catch (URISyntaxException e) {
			Directory.LOGGER.debug(Directory.MARKER_DEBUG_SPARQL, e.toString());
		}
	}
	
	public static void setQuerySparqlEndpoint(String queryEndpoint) {
		try {
			if(queryEndpoint!=null) 
				SPARQLEndpoint.queryEnpoint = new URI(queryEndpoint);
		} catch (URISyntaxException e) {
			Directory.LOGGER.debug(Directory.MARKER_DEBUG_SPARQL, e.toString());
		}
	}
	
	// -- Methods
	
	public static ByteArrayOutputStream sendUpdateQuery(String query) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			if(updateEnpoint==null)
				throw new RemoteException("SPARQL for updating data is not configured");
			HttpURLConnection connection = prepareUpdatePOST(updateEnpoint, query.getBytes(), Utils.MIME_TURTLE, "application/sparql-update");
			int code = connection.getResponseCode();
			if(200<=code && code<300) {
				connection.getInputStream().transferTo(output);
			}else {	
				connection.getErrorStream().transferTo(output);
			}
			connection.disconnect();
		}catch (IOException e) {
			Directory.LOGGER.debug(Directory.MARKER_DEBUG_SPARQL, e.toString());
		} 
		
		return output;
	}
	
	private static HttpURLConnection prepareUpdatePOST(URI endpointURL, byte[] query, String accept, String contentType) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) endpointURL.toURL().openConnection();
		connection.setRequestMethod(Utils.METHOD_POST);
		connection.addRequestProperty(Utils.HEADER_ACCEPT, accept);
		connection.addRequestProperty(Utils.HEADER_CONTENT_TYPE, contentType);
		connection.setDoOutput(true);
		connection.getOutputStream().write(query, 0, query.length);
		return connection;
	}
	

	
	public static ByteArrayOutputStream sendQueryStream(String query, String mimetype) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		HttpURLConnection connection = null;
		try {
			if(queryEnpoint==null)
				throw new RemoteException("SPARQL for retrieving data is not configured");
			if(queryUsingGET) {
				connection = prepareGET(queryEnpoint, query, mimetype);
			}else {
				connection = preparePOST(queryEnpoint, query.getBytes(), mimetype);
			}
			
			int code = connection.getResponseCode();
			if(200<=code && code<300) {
				connection.getInputStream().transferTo(output);
			}else {	
				connection.getErrorStream().transferTo(output);
			}
			connection.disconnect();
		}catch (Exception e) {
			throw new RemoteException(e.toString());
		} 
		return output;
	}
	
	public static String sendQueryString(String query, String mimetype) {
		ByteArrayOutputStream output = sendQueryStream( query,  mimetype);
		return new String(output.toByteArray());
	}
	
	public static OutputStream sendQuery(String query, String mimetype) {
		return sendQueryStream( query,  mimetype);
	}
	
	
	private static HttpURLConnection prepareGET(URI endpointURL, String query, String mimetype) throws IOException, URISyntaxException {
		URI enpointFormatted = new URI(Utils.buildMessage(endpointURL.toString(),"?query=",URLEncoder.encode(query, StandardCharsets.UTF_8.toString())));
		HttpURLConnection connection = (HttpURLConnection) enpointFormatted.toURL().openConnection();
		connection.setRequestMethod(Utils.METHOD_GET);
		connection.addRequestProperty(Utils.HEADER_ACCEPT, mimetype);
		connection.setDoOutput(true);
		return connection;
	}
	
	private static HttpURLConnection preparePOST(URI endpointURL, byte[] query, String mimetype) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) endpointURL.toURL().openConnection();
		connection.setRequestMethod(Utils.METHOD_POST);
		connection.addRequestProperty(Utils.HEADER_ACCEPT, mimetype);
		connection.addRequestProperty(Utils.HEADER_CONTENT_TYPE, "application/sparql-query");
		connection.setDoOutput(true);
		connection.getOutputStream().write(query, 0, query.length);
		return connection;
	}
}
