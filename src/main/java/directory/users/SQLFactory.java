package directory.users;

import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import javax.naming.ConfigurationException;

import directory.Directory;
import directory.Utils;

public class SQLFactory {

	// -- Attributes
	
	// -- Constructor
	private SQLFactory() {
		super();
	}
	
	// -- Methods
	
	// Initialize database with default credentials
	
	public static int dbStatus() throws ConfigurationException {
		int status = -1;
		status = executeStatusQuery("SELECT COUNT(name) AS count FROM sqlite_master WHERE name='bees'");
		if(status==1) {
			status = executeStatusQuery("SELECT COUNT(user) AS count FROM bees");
			if(status!=1)
				insertUser("root", "root");
		}else{
			createTable();
		}
		return status;
	}
	
	private static void createTable() {
		runSQLStatement("CREATE TABLE IF NOT EXISTS bees (user text primary key, secret text, modified datetime);");
	}
	
	public static int executeStatusQuery(String sql) {
		Connection connection = getConnection();
		int statusOk = -1;
		try {
			if(connection!=null) {
				
				PreparedStatement st = connection.prepareStatement(sql);
				ResultSet rs = st.executeQuery();
				statusOk = rs.getInt("count");
				rs.close();
				st.close();
				connection.close();
			}else {
				Directory.LOGGER.debug("Database error", Utils.buildMessage("Error running query: ", sql));
				System.out.println("error!!!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return statusOk;
	}
	
	public static void runSQLStatement(String sql) {
		Connection connection = getConnection();
		try {
			if(connection!=null) {
				Statement st = connection.createStatement();
				st.execute(sql);
				st.close();
				connection.close();
			}else {
				Directory.LOGGER.debug("Database error", Utils.buildMessage("Error running query: ", sql));
				System.out.println("error!!!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	// CRUD
	
	private static final void insertUser(String user, String secret) {
		try {
			Connection connection = getConnection();
			if(connection!=null) {
				PreparedStatement st = connection.prepareStatement("insert into bees (user, secret, modified) values (?,?,?)");
	            st.setString(1, user);
	            st.setString(2, secret);
	            Date date = new Date(Calendar.getInstance().getTime().getTime());
	            st.setDate(3, date);
	            st.execute();
	            st.close();
			}else {
				Directory.LOGGER.debug("Database error", "Error running query for inserting new user: ");
				System.out.println("error!!!");
			}
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }

	}
	
	public static final Boolean existsUser(String user) {
		Boolean exist = false;
		try {
			Connection connection = getConnection();
			if(connection!=null) {
				PreparedStatement st = connection.prepareStatement("SELECT user FROM bees");
				ResultSet rs = st.executeQuery();
				exist = rs.getString("user").equals(user);
				rs.close();
				st.close();
				connection.close();
			}else {
				Directory.LOGGER.debug("Database error", Utils.buildMessage("Error running query to check if user exists, provided user: ", user));
				System.out.println("error!!!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return exist;
	}
	
	public static final Boolean existsUser(String user, String secret) {
		Boolean exist = false;
		try {
			Connection connection = getConnection();
			if(connection!=null) {
				PreparedStatement st = connection.prepareStatement("SELECT user, secret FROM bees LIMIT 1");
				ResultSet rs = st.executeQuery();
				while (rs.next()) {
					String userStored = rs.getString("user");
					String passwordStored = rs.getString("secret");
					exist = userStored.equals(user) && passwordStored.equals(secret);
				}
				rs.close();
				st.close();
				connection.close();
			}else {
				Directory.LOGGER.debug("Database error", Utils.buildMessage("Error running query to check if user exists with password, provided user: ", user));
				System.out.println("error!!!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return exist;
	}
	
	private static final void updateUser(String user, String secret) {
	/*	try {
          
			insertUser( user,  secret);
        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
*/
	}
	
	// -- Ancillary methods
	
	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	private static Connection getConnection(){
		Connection connection = null;
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:./storage.db");
		 }catch (SQLException ex) {
			 Directory.LOGGER.debug("Database error", ex.toString());
			 ex.printStackTrace();
		 }
		return connection;
	}
	
}
