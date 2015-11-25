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

    static {
        System.out.println("Verify: static initialization for properties");
		Constants.setDatabaseVariables();
		Constants.setGCMVariables();
    }

  	public Register() {
		System.out.println("constructor is called");
  	}

  	public void init() throws ServletException
  	{
      	// Do required initialization
      	System.out.println("init is getting called");
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
			Connection con = DriverManager.getConnection(dburl, Constants.sdbUsername, Constants.sdbPassword);
			PreparedStatement selectStatement = con.prepareStatement(SELECT_BY_PHONE);
			selectStatement.setString(1, registrationRequest.getPhoneNumber());
			ResultSet rs = selectStatement.executeQuery();
			if (rs.first()) {
				errorCode = PHONE_ALREADY_REGISTERED;
				errorMessage = "Phone already registered";
			}
			rs.close();
			PreparedStatement insertStatement = con.prepareStatement(INSERT_CLIENT);
			String clientUserId = UUID.randomUUID().toString();
			insertStatement.setString(1, clientUserId);
			insertStatement.setString(2, registrationRequest.getRegistrationId());
			insertStatement.setString(3, registrationRequest.getPhoneNumber());
			insertStatement.setString(4, registrationRequest.getPublicKey());
			insertStatement.execute();
		} catch (Exception ex) {
            errorMessage = ex.getMessage();
            errorCode = DATABASE_CONNECTION_FAILED;
			System.out.println("threw an exception connection to " + url);
			ex.printStackTrace();
		}
        String json = String.format(JSON_FORMAT, registrationRequest.getRegistrationId(), errorCode, errorMessage);
        PrintWriter out = response.getWriter();
        out.println(json);
        System.out.println(json);
  }

  public String getParam(String pair) throws UnsupportedEncodingException {
  	int idx = pair.indexOf("=");
	return URLDecoder.decode(pair.substring(0, idx), "UTF-8");
  }

  public String getValue(String pair) throws UnsupportedEncodingException {
  	int idx = pair.indexOf("=");
	return URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
  }


  
  public void destroy()
  {
      // do nothing.
  }
} 
