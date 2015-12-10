package com.visibleautomation.verify;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.io.IOException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.simple.parser.ParseException;
import com.visibleautomation.util.ServletUtil;

/**
 * Servlet that saves the traceroute under the reqest ID
 */
public class SaveTraceroute extends HttpServlet {
	private static final String UPDATE_CLIENT_TRACEROUTE = "UPDATE request set CLIENT_TRACEROUTE=? where REQUEST_ID=?";
	private static final String REQUEST_ID = "requestId";
	private static Connection sDBConnection;

	/**
     * static initialization: load the properties into constants, load the JDBC devier, and open the connection to the database.
     */
    static {
        System.out.println("Verify: static initialization for sql.properties");
		Constants.setDatabaseVariables();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String dbConnection = String.format(Constants.DB_CONNECTION_FORMAT, Constants.getDBDatabase());
			sDBConnection = DriverManager.getConnection(dbConnection, Constants.getDBUsername(), Constants.getDBPassword());
		} catch (Exception ex) {
			System.out.println("failed to initialize database " + ex.getMessage());
		}
    }

	public SaveTraceroute() {
		System.out.println("constructor is called");
  	}

	public void init() throws ServletException {
      	// Do required initialization
      	System.out.println("init is getting called");
	}

	
	/**
     * standard servlet post method
     */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      	// Set response content type
      	response.setContentType("application/json");
		System.out.println("doPost is getting called query = " + request.getQueryString());
		String queryString = request.getQueryString();
		String requestId = null;
		String[] pairs = queryString.split("&");
		for (int i = 0; i < pairs.length; i++) {
			String pair = pairs[i];
			String param = ServletUtil.getParam(pair);
			if (param.equals(REQUEST_ID)) {
				requestId = ServletUtil.getValue(pair);
			}
		}
		System.out.println("SaveTraceroute: requestId = " + requestId);
		if (requestId != null) {
        	String postData = ServletUtil.readPostData(request);
        	System.out.println("post data = " + postData);
			JSONObject jsonObject = new JSONObject(postData);
			JSONArray tracerouteArray = jsonObject.getJSONArray("traceroute");
			String traceroute = getTracerouteString(tracerouteArray);
			try {	
				saveTraceroute(requestId, traceroute);
			} catch (SQLException sqlex) {
				System.out.println("exception writing traceroute for " + requestId);
				sqlex.printStackTrace();
			}
		}
	}

	public static String getTracerouteString(JSONArray tracerouteArray) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < tracerouteArray.length(); i++) {
			sb.append(tracerouteArray.getString(i));
			sb.append("\n");
		}
		return sb.toString();
	}
			


	// initialize the request by storing the request ID and the timestamp.
	public static void saveTraceroute(String requestId, String traceroute) throws SQLException {
		PreparedStatement updateStatement = sDBConnection.prepareStatement(UPDATE_CLIENT_TRACEROUTE);
		updateStatement.setString(1, requestId);
		updateStatement.setString(2, traceroute);
		updateStatement.execute();
	}
  
	public void destroy() {
      	// do nothing.
	}
} 
