package directory.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.MarkerFactory;
import directory.Directory;

public abstract class AbstractSQLFactory {

	// -- Attributes

	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			Directory.LOGGER.debug(MarkerFactory.getMarker(Directory.MARKER_DEBUG_SECURITY), e.toString());
		}
	}

	// -- Constructor
	public AbstractSQLFactory() {
		super();
	}

	// -- Methods

	protected static Connection getConnection() {
		Connection connection = null;
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:./storage.db");
		} catch (SQLException ex) {
			Directory.LOGGER.debug(MarkerFactory.getMarker(Directory.MARKER_DEBUG_SECURITY), ex.toString());
		}
		return connection;
	}

	// Initialize database with default credentials

	public static int executeStatusQuery(String sql) {
		int statusOk = -1;
		try (Connection connection = getConnection()) {
			if (connection != null) {
				try (PreparedStatement st = connection.prepareStatement(sql)) {
					try (ResultSet rs = st.executeQuery()) {
						statusOk = rs.getInt("count");
					}
				}
			} else {
				Directory.LOGGER.debug(Directory.MARKER_DEBUG_SQL,
						"Connection seems not available, error running query: {0}", sql);
			}
		} catch (SQLException e) {
			Directory.LOGGER.debug(Directory.MARKER_DEBUG_SQL, e.toString());
		}
		return statusOk;
	}

	public static void runSQLStatement(String sql) {
		try (Connection connection = getConnection()) {
			if (connection != null) {
				try (Statement st = connection.createStatement()) {
					st.execute(sql);
				}
			} else {
				Directory.LOGGER.debug(Directory.MARKER_DEBUG_SQL,
						"Connection seems not available, error running query: {0}", sql);
			}
		} catch (SQLException e) {
			Directory.LOGGER.debug(Directory.MARKER_DEBUG_SQL, e.toString());
		}

	}

}
