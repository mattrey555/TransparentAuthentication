package org.example.login;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class Constants {
	public static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/third_party";
	public static final String DB_CONNECTION_FORMAT = "jdbc:mysql://localhost:3306/%s";
	private static final String SQL_PROPERTIES_FILE = "sql.properties";
	private static final String SQL_USERNAME_PROPERTY = "sql.username";
	private static final String SQL_PASSWORD_PROPERTY = "sql.password";
	private static final String SQL_DATABASE_PROPERTY = "sql.database";
	private static final String URL_PARAM_USERNAME = "username";
	private static final String URL_PARAM_PASSWORD = "password";

    private static String sdbUsername = null;
	private static String sdbPassword = null;
	private static String sdbDatabase = null;


	private static final String NET_PROPERTIES_FILE = "network.properties";
	private static final String NET_VERIFY_ADDRESS = "network.verify_address";
	private static final String NET_CLIENT_ID = "network.client_id";
	private static final String NET_MAX_HOPS = "network.max_hops";
	private static final String NET_TIMEOUT_MSEC = "network.timeout_msec";

    private static String sNetVerifyAddress = null;
	private static int sNetClientId = 0;
	private static int sNetMaxHops = 0;
	private static int sNetTimeoutMsec = 0;

	// read the SQL properties file and get the database variables	
	public static void setDatabaseVariables() {
		System.out.println("ProcessLoginServlet: static initialization for sql.properties");
		Properties sqlProperties = new Properties();
	    InputStream is = null;
		try {
			is = ProcessLoginServlet.class.getClassLoader().getResourceAsStream(SQL_PROPERTIES_FILE);
			if (is != null) {
				sqlProperties.load(is);
				sdbUsername = sqlProperties.getProperty(SQL_USERNAME_PROPERTY);
				sdbPassword = sqlProperties.getProperty(SQL_PASSWORD_PROPERTY);
				sdbDatabase = sqlProperties.getProperty(SQL_DATABASE_PROPERTY);
				System.out.println("ProcessLoginServlet: sql.properties file loaded successfully username = " + 
								   sdbUsername + " password = " + sdbPassword + " database = " + sdbDatabase);
			} else {
				System.out.println("failed to load " + SQL_PROPERTIES_FILE);
			}
		} catch (IOException ioex) {
			System.out.println("ProcessLoginServlet: failed to load properties file " + SQL_PROPERTIES_FILE + "message = " + ioex.getMessage());
		} catch (Exception ex) {
			System.out.println("ProcessLoginServlet: failed to load properties file " + SQL_PROPERTIES_FILE + "message = " + ex.getMessage());
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException ioex) {
			}
		}
	}

	public static String getDBUsername() {
		return sdbUsername;
	}

	public static String getDBPassword() {
		return sdbPassword;
    }

	public static String getDBDatabase() {
		return sdbDatabase;
    }
	
	public static void setNetworkVariables() {
		System.out.println("ProcessLoginServlet: static initialization for network.properties");
		Properties sqlProperties = new Properties();
	    InputStream is = null;
		try {
			is = ProcessLoginServlet.class.getClassLoader().getResourceAsStream(NET_PROPERTIES_FILE);
			if (is != null) {
				sqlProperties.load(is);
				sNetVerifyAddress = sqlProperties.getProperty(NET_VERIFY_ADDRESS);
				sNetClientId = Integer.parseInt(sqlProperties.getProperty(NET_VERIFY_ADDRESS));
				sNetMaxHops = Integer.parseInt(sqlProperties.getProperty(NET_MAX_HOPS));
				sNetTimeoutMsec = Integer.parseInt(sqlProperties.getProperty(NET_TIMEOUT_MSEC));
				System.out.println("ProcessLoginServlet: network.properties file loaded successfully verify address = " + sNetVerifyAddress + 
								   " client id = " + sNetClientId + 
								   " max hops = " + sNetMaxHops  + 
								   " timeout msec = " + sNetTimeoutMsec);
			} else {
				System.out.println("failed to load " + NET_PROPERTIES_FILE);
			}
		} catch (IOException ioex) {
			System.out.println("ProcessLoginServlet: failed to load properties file " + NET_PROPERTIES_FILE + "message = " + ioex.getMessage());
		} catch (Exception ex) {
			System.out.println("ProcessLoginServlet: failed to load properties file " + NET_PROPERTIES_FILE + "message = " + ex.getMessage());
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException ioex) {
			}
		}
	}

	public static String getVerifyAddress() {
		return sNetVerifyAddress;
	}

	public static int getClientId() {
		return sNetClientId;
    }

	public static int getMaxHops() {
		return sNetMaxHops;
    }


	public static int getTimeoutMsec() {
		return sNetTimeoutMsec;
    }



}
