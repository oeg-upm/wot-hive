package directory.storage;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import org.slf4j.MarkerFactory;
import directory.Directory;

public class CredentialsSQLFactory extends AbstractSQLFactory{

	// -- Attributes
	
	// -- Constructor
	
	private CredentialsSQLFactory() {
		super();
	}
	
	// -- Methods
	
	
	// CRUD
	
	public static final void updateCredentials(String user, String secret) {
        Boolean correct = false;
		try (Connection connection = getConnection()){
			if(connection!=null) {
				try(PreparedStatement st = connection.prepareStatement("insert into bees (user, secret, modified) values (?,?,?)")){
					st.setString(1, user);
		            st.setString(2, secret);
		            st.setDate(3, new Date(Calendar.getInstance().getTime().getTime()));
		            st.execute();
		            correct = true;
	            }
			}else {
				Directory.LOGGER.debug(Directory.MARKER_DEBUG_SQL,"Connection seems not available, error updating credentials");
			}
		} catch (SQLException e) {
			Directory.LOGGER.debug(Directory.MARKER_DEBUG_SQL, e.toString());
		}
		if(correct)
			cleanTable();
	}
	
	public static final Boolean existsUser(String user) {
		Boolean exist = user != null && !user.isEmpty();
		if (exist) {
			String storedUser = getStoredCredentials(false)[0];
			exist = storedUser != null && storedUser.equals(user);
		} else {
			Directory.LOGGER.debug(MarkerFactory.getMarker(Directory.MARKER_DEBUG_SECURITY),"Error running query to check if user exists, provided user: {0}", user);
		}
		return exist;
	}
	
	public static final Boolean existsUser(String user, String secret) {
		Boolean exist = user!=null && !user.isEmpty() && secret!=null && !secret.isEmpty();
		if(exist) {
			String[] credentials = getStoredCredentials(true);
			exist = credentials[0]!= null && credentials[1]!=null && credentials[0].equals(user) && credentials[1].equals(secret);
		}else {
			Directory.LOGGER.debug(Directory.MARKER_DEBUG_SECURITY, "Error connection to local db is not ready");
		}
		return exist;
	}
	
	public static final String[] getStoredCredentials(Boolean getSecret) {
		String[] credentials = new String[3];
		try(Connection connection = getConnection()){
					if(connection!=null) {
						try(PreparedStatement st = connection.prepareStatement("SELECT user, secret, modified  FROM bees")){
							try(ResultSet rs = st.executeQuery()){
								credentials[0] = rs.getString("user");
								if(getSecret)
									credentials[1] = rs.getString("secret");
								credentials[2] = rs.getString("modified");
							}
						}
					}else {
						Directory.LOGGER.debug(MarkerFactory.getMarker(Directory.MARKER_DEBUG_SECURITY), "Error connection to local db is not ready");
					}			
		} catch (SQLException e) {
			Directory.LOGGER.debug(MarkerFactory.getMarker(Directory.MARKER_DEBUG_SECURITY), e.toString());
		}
		return credentials;
	}
	
	
	// -- Ancillary methods
	
	// Initialize database with default credentials
	
		public static int dbStatus() {
			int status = -1;
			status = executeStatusQuery("SELECT COUNT(name) AS count FROM sqlite_master WHERE name='bees'");
			if(status==1) {
				status = executeStatusQuery("SELECT COUNT(user) AS count FROM bees");
				if(status!=1)
					updateCredentials("root", "root");
			}else{
				createTable();
			}
			return status;
		}
		
		private static void createTable() {
			runSQLStatement("CREATE TABLE IF NOT EXISTS bees (user text primary key, secret text, modified datetime);");
		}
		
		private static void cleanTable() {
			int status = executeStatusQuery("SELECT COUNT(user) AS count FROM bees");
			if(status>1)
				runSQLStatement("DELETE FROM bees WHERE user IN (SELECT user FROM bees ORDER BY modified ASC LIMIT 1)");
		}
		
	
}
