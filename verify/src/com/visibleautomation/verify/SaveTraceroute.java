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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Servlet that saves the traceroute under the reqest ID
 */
public class SaveTraceroute extends HttpServlet {
	private static final String UPDATE_CLIENT_TRACEROUTE = "UPDATE request set CLIENT_TRACEROUTE=? where REQUEST_ID=?";
	private static final String REQUEST_ID = "requestId";
	private static Connection sDBConnection;
	private static Logger logger;

	/**
     * static initialization: load the properties into constants, load the JDBC devier, and open the connection to the database.
     */
    static {
		Logger logger = LogManager.getLogger(SaveTraceroute.class);
        logger.debug("Verify: static initialization for sql.properties");
		Constants.setDatabaseVariables();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String dbConnection = String.format(Constants.DB_CONNECTION_FORMAT, Constants.getDBDatabase());
			sDBConnection = DriverManager.getConnection(dbConnection, Constants.getDBUsername(), Constants.getDBPassword());
		} catch (Exception ex) {
			logger.debug("failed to initialize database " + ex.getMessage());
		}
    }

	public SaveTraceroute() {
		logger.debug("constructor is called");
  	}

	public void init() throws ServletException {
      	// Do required initialization
      	logger.debug("init is getting called");
	}

	
	/**
     * standard servlet post method
     */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      	// Set response content type
      	response.setContentType("application/json");
		logger.debug("doPost is getting called query = " + request.getQueryString());
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
		logger.debug("SaveTraceroute: requestId = " + requestId);
		if (requestId != null) {
        	String postData = ServletUtil.readPostData(request);
        	logger.debug("post data = " + postData);
			JSONObject jsonObject = new JSONObject(postData);
			JSONArray tracerouteArray = jsonObject.getJSONArray("traceroute");
			String traceroute = getTracerouteString(tracerouteArray);
			try {	
				saveTraceroute(requestId, traceroute);
			} catch (SQLException sqlex) {
				logger.error("exception writing traceroute for " + requestId);
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
      	logger.debug("destroy called");
	}
} 
