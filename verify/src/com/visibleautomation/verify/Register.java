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

// user (ID INT, CLIENT_USER_ID VARCHAR(64) NOT NULL, USER_ID VARCHAR(64) NOT NULL, phone_number varchar(64) PRIMARY KEY(ID)
// client (ID INT, CLIENT_ID VARCHAR(64) NOT NULL, CLIENT_USER_ID VARCHAR(64) NOT NULL, PRIMARY KEY (ID));
public class Register extends HttpServlet {
	private static final int SUCCESS = 0;
	private static final int BAD_URL = 1;
	private static final int DATABASE_CONNECTION_FAILED = 2;
	private static final int PHONE_ALREADY_REGISTERED = 3;
	private static final String JSON_FORMAT = "{ \"registrationId\" : \"%s\", \"statusCode\" : %d, \"errorMessage\" : \"%s\" }";
	private static final String SELECT_BY_PHONE = "select * from user where phone_number=?";
	private static final String INSERT_CLIENT = "insert into user values (0,?,?,?)";

	// url is of the form <servlet-name>/register?registrationId=XXX&phoneNumber=XXX
  	private static final String REGISTRATION_ID = "registrationId"; 
  	private static final String PHONE_NUMBER = "phoneNumber"; 

  	public Register() {
		System.out.println("constructor is called");
  	}

  	public void init() throws ServletException
  	{
      	// Do required initialization
      	System.out.println("init is getting called");
  	}

  	public void doGet(HttpServletRequest 	request,
                      HttpServletResponse 	response)
		throws ServletException, IOException {

      	// Set response content type
      	String urlStr = request.getRequestURL().toString();
      	URL url = new URL(urlStr); 
      	response.setContentType("application/json");
      	System.out.println("doGet is getting called query = " + request.getQueryString());
      	String[] pairs = request.getQueryString().split("&");
	  	String phoneNumber = null;
	  	String registrationId = null;

		// extract the phone number and registration ID.  phone number is visible to the 3rd party
		// registration ID is not.
		String errorMessage = "Success";
		int errorCode = SUCCESS;
	  	try {
		  	for (String pair : pairs) {
				String param = getParam(pair);
				String value = getValue(pair);
				if (param.equals(PHONE_NUMBER)) {
					phoneNumber = value;
				}
				if (param.equals(REGISTRATION_ID)) {
					registrationId = value;
				}
		  	}
		} catch (Exception ex) {
			errorMessage = ex.getMessage();
			errorCode = BAD_URL;
			phoneNumber = null;
			registrationId = null;
		}
	  	if ((phoneNumber != null) && (registrationId != null)) {
	
		  	String dburl = "jdbc:mysql://localhost:3306/transparent_authentication";
		  	try {
		    	Class.forName("com.mysql.jdbc.Driver");
				Connection con = DriverManager.getConnection(dburl, "verify", "FiatX1/9");
				PreparedStatement selectStatement = con.prepareStatement(SELECT_BY_PHONE);
				selectStatement.setString(1, phoneNumber);
				ResultSet rs = selectStatement.executeQuery();
				if (rs.first()) {
					errorCode = PHONE_ALREADY_REGISTERED;
					errorMessage = "Phone already registered";
				}
				rs.close();
				PreparedStatement insertStatement = con.prepareStatement(INSERT_CLIENT);
				insertStatement.setString(1, registrationId);
				insertStatement.setString(2, UUID.randomUUID().toString());
				insertStatement.setString(3, phoneNumber);
				insertStatement.execute();
		  	} catch (Exception ex) {
				errorMessage = ex.getMessage();
				errorCode = DATABASE_CONNECTION_FAILED;
				System.out.println("threw an exception connection to " + url);
				ex.printStackTrace();
		  	}
		} else {
			errorMessage = "must specify a phone number and registration ID";
			errorCode = BAD_URL;
	    }
		String json = String.format(JSON_FORMAT, registrationId, errorCode, errorMessage);
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
