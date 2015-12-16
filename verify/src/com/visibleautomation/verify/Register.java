package com.visibleautomation.verify;
import java.io.IOException;
import java.lang.Class;

import java.io.*;
import javax.servlet.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.io.UnsupportedEncodingException;

import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import javax.servlet.http.*;
import com.google.gson.Gson;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Registration servlet: receives request from phone with phone number and GCM client ID for push notifications
 */
// user (ID INT, CLIENT_USER_ID VARCHAR(64) NOT NULL, USER_ID VARCHAR(64) NOT NULL, phone_number varchar(64) PRIMARY KEY(ID)
// client (ID INT, CLIENT_ID VARCHAR(64) NOT NULL, CLIENT_USER_ID VARCHAR(64) NOT NULL, PRIMARY KEY (ID));
public class Register extends HttpServlet {
	private static final int SUCCESS = 0;
	private static final int BAD_URL = 1;
	private static final int DATABASE_CONNECTION_FAILED = 2;
	private static final int PHONE_ALREADY_REGISTERED = 3;
	private static final String JSON_FORMAT = "{ \"registrationId\" : \"%s\", \"statusCode\" : %d, \"errorMessage\" : \"%s\" }";
	private static final String SELECT_BY_PHONE = "select * from user where phone_number=?";
	private static final String INSERT_CLIENT = "insert into user values (0,?,?,?,?)";
	private static final String DELETE_CLIENT = "delete from user where phone_number=?";
	private static Logger logger;
	private static Connection sDBConnection;

    static {
		try {
			logger = LogManager.getLogger(Register.class);
			logger.debug(" static initialization for properties");
			Constants.setDatabaseVariables();
			Constants.setGCMVariables();
			Class.forName("com.mysql.jdbc.Driver");
			String dbConnection = String.format(Constants.DB_CONNECTION_FORMAT, Constants.getDBDatabase());
			sDBConnection = DriverManager.getConnection(dbConnection, Constants.getDBUsername(), Constants.getDBPassword());
		} catch (Exception ex) {
			logger.error("threw an exception on static initialization", ex);
		}
    }

  	public Register() {
		logger.debug("constructor is called");
  	}

  	public void init() throws ServletException {
      	// Do required initialization
      	logger.debug("init is getting called");
  	}

  	public void doPost(HttpServletRequest 	request,
                       HttpServletResponse 	response)
		throws ServletException, IOException {

      	// Set response content type
      	String urlStr = request.getRequestURL().toString();
      	URL url = new URL(urlStr); 
      	response.setContentType("application/json");
        BufferedReader br = request.getReader();
		Gson gson = new Gson();
        RegistrationRequest registrationRequest = gson.fromJson(br, RegistrationRequest.class);

		String dburl = String.format(Constants.DB_CONNECTION_FORMAT, Constants.sdbDatabase);
	    int errorCode = SUCCESS;
	    String errorMessage = null;
								    
		try {
			Class.forName("com.mysql.jdbc.Driver");
			deletePreviousRegistrationRequest(sDBConnection,  registrationRequest);
			String clientUserId = UUID.randomUUID().toString();
			insertRegistrationRequest(sDBConnection, clientUserId, registrationRequest);
		} catch (Exception ex) {
            errorMessage = ex.getMessage();
            errorCode = DATABASE_CONNECTION_FAILED;
			logger.error("threw an exception connection to " + url);
			ex.printStackTrace();
		}
        String json = String.format(JSON_FORMAT, registrationRequest.getRegistrationId(), errorCode, errorMessage);
        PrintWriter out = response.getWriter();
        out.println(json);
        logger.debug(json);
  	}
 
    /**
	 * insert the registration request into the database
	 * @param con database connection
	 * @param clientUserId random UUID to identify user.
	 * @param registrationRequest GCM registration ID, mobile phone number, and RSA public key
	 */
	private void insertRegistrationRequest(Connection con, String clientUserId, RegistrationRequest registrationRequest) throws Exception {
		PreparedStatement insertStatement = con.prepareStatement(INSERT_CLIENT);
		insertStatement.setString(1, clientUserId);
		insertStatement.setString(2, registrationRequest.getRegistrationId());
		insertStatement.setString(3, registrationRequest.getPhoneNumber());
		insertStatement.setString(4, registrationRequest.getPublicKey());
		insertStatement.execute();
	}

	/**
	 * delete the previous registration request for this user.
	 * @param con database connection
	 * @param registrationRequest GCM registration ID, mobile phone number, and RSA public key
	 */
	 
	private void deletePreviousRegistrationRequest(Connection con,  RegistrationRequest registrationRequest) throws Exception {
		PreparedStatement deleteStatement = con.prepareStatement(DELETE_CLIENT);
		deleteStatement.setString(1, registrationRequest.getPhoneNumber());
		deleteStatement.execute();
	}


  	public void destroy() {
      // do nothing.
  	}
} 
