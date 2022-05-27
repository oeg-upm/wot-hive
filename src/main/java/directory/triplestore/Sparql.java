package directory.triplestore;

import java.io.ByteArrayOutputStream;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.jetty.client.HttpClient;

import directory.Directory;
import directory.Utils;
import directory.exceptions.RemoteSparqlEndpointException;
import sparql.streamline.core.SparqlEndpoint;
import sparql.streamline.exception.SparqlConfigurationException;
import sparql.streamline.exception.SparqlQuerySyntaxException;
import sparql.streamline.exception.SparqlRemoteEndpointException;

public class Sparql {

	/*
	 * static { startEmbeddedSparql("hive"); }
	 */

	private Sparql() {
		super();
	}

	public static ResultsFormat guess(String str) {
		return ResultsFormat.lookup(str);
	}
	//

	// query methods

	public static ByteArrayOutputStream query(String sparql, ResultsFormat format){
		try {
			return Directory.getConfiguration().getTriplestore().getSparqlEndpoint().query(sparql, format);
		} catch (Exception e) {
			throw new RemoteSparqlEndpointException(e.toString());
		}
	}

	public static void update(String sparql) {
		try {
			Directory.getConfiguration().getTriplestore().getSparqlEndpoint().update(sparql);
		} catch (Exception e) {
			throw new RemoteSparqlEndpointException(e.toString());
		}
	}

}
