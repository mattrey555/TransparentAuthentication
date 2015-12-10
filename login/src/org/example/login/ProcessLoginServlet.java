package org.example.login;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.RequestDispatcher;
import org.json.JSONObject;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import com.visibleautomation.util.StringUtil;
import com.visibleautomation.util.ServletUtil;
import com.visibleautomation.util.ProcessUtil;

/**
 * Servlet implementation to process the login and request verification from the handset
 */
public class ProcessLoginServlet extends HttpServlet {
	private static final String SELECT_USERNAME_PASSWORD = "SELECT * FROM USER WHERE USER_ID=? AND PWD=?";
	private static final String UPDATE_TOKEN_AND_SESSION_ID = "UPDATE SESSION SET TOKEN=?,VERIFY_SESSION_ID=?  WHERE SESSION_ID=?";
	private static final String VERIFY_URL_FORMAT = "https://%s/verify/verify?phoneNumber=%s&terminalIPAddress=%s&siteIPAddress=%s&clientId=%d&maxHops=%d&timeoutMsec=%d";
	private static final String POST_TRACEROUTE_URL_FORMAT = "https://%s/verify/postTraceroute?requestId=%s";
	private static final String IP_REGEXP = "\"[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*\"";
	private static final String TRACEROUTE_CMD = "traceroute -n --module=udp --queries=1 %s -w 0.25 --back | grep -v \"\\*\" | grep -o " + IP_REGEXP;
	private static final String JSON_TAG_TERMINAL_IP_ADDRESS = "terminalIPAddress";
	private static final String JSON_TAG_TRACEROUTE = "traceroute";
   	private static final String URL_PARAM_USERNAME = "username";
	private static final String URL_PARAM_PASSWORD = "password";
	private static final String URL_PARAM_SESSIONID = "sessionId";
	private static Connection sDBConnection;
	
	static {
		System.out.println("ProcessLoginServlet: static initialization for sql.properties");
		Constants.setDatabaseVariables();
		Constants.setNetworkVariables();
		try {
		   Class.forName("com.mysql.jdbc.Driver");
		   String dbConnection = String.format(Constants.DB_CONNECTION_FORMAT, Constants.getDBDatabase());
		   sDBConnection = DriverManager.getConnection(dbConnection, Constants.getDBUsername(), Constants.getDBPassword());
		} catch (Exception ex) {
			System.out.println("threw an exception initializing the database " + ex.getMessage());
		}
	}
	static {
	    //for localhost testing only
	    javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
	    new javax.net.ssl.HostnameVerifier(){

	        public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
	            if (hostname.equals("localhost")) {
	                return true;
	            }
	            return false;
	        }
	    });
	}

    public ProcessLoginServlet() {
		System.out.println("ProcessLoginServlet: constructor  called");
    }

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {

		try {	    
			String username  = request.getParameter(URL_PARAM_USERNAME);
			String password  = request.getParameter(URL_PARAM_PASSWORD);
			String sessionId  = request.getParameter(URL_PARAM_SESSIONID);
			System.out.println("username = " + username + " password = " + password + " sessionID = " + sessionId);
			String phoneNumber = verifyLogin(username, password);
			if (phoneNumber == null) {
				response.sendRedirect("login_failed.jsp");
			} else {
				
				// get the IP Address of the accessing terminal
				String terminalIPAddress = request.getRemoteAddr();

				// get this server's external IP
				String incomingURLStr = request.getRequestURL().toString();
				URL incomingURL = new URL(incomingURLStr);
				String serverAddress = incomingURL.getHost();
				InetAddress siteInetAddress = InetAddress.getByName(incomingURL.getHost());
				String siteIPAddress = siteInetAddress.getHostAddress();

				// make the request to the verify URL.  Post the traceroute to the requesting IP 
				// TODO: the first request should send the terminal IP, site IP, user ID with a unique ID
				// the second request should send the traceroute, so we don't have to wait for this traceroute
				// before getting the others.
				String verifyURLStr = String.format(VERIFY_URL_FORMAT, Constants.getVerifyAddress(), phoneNumber, terminalIPAddress, 
												    siteIPAddress, Constants.getClientId(), 
												    Constants.getMaxHops(), Constants.getTimeoutMsec());
				System.out.println(verifyURLStr);
				URL verifyURL = new URL(verifyURLStr);
				HttpURLConnection verifyUrlConnection = ServletUtil.postUrlString(verifyURL, createPayload(terminalIPAddress));
				HandsetURLAndToken handsetURLAndToken = new HandsetURLAndToken(verifyUrlConnection.getInputStream()); 
				if (handsetURLAndToken.getError().equals("SUCCESS")) {
					handsetURLAndToken.saveToken(sDBConnection, sessionId);
					System.out.println("success token = " + handsetURLAndToken.getToken() + 
									   " handsetURL = " + handsetURLAndToken.getHandsetURL());
					RequestDispatcher requestDispatcher = request.getRequestDispatcher("/verifying.jsp");
					request.setAttribute("handsetURL", handsetURLAndToken.getHandsetURL());
					request.setAttribute("sessionId", sessionId);
					requestDispatcher.forward(request, response);
				} else {
					System.out.println("failure getting handset URL and token"); 
					response.sendRedirect("verification_failed.jsp");
				}

				// post the traceroute from the terminal to this site, and send it to the verifier
				postForwardTraceRoute(handsetURLAndToken.getRequestId(), terminalIPAddress);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// given the username and password from the UI, return the userID if it exists, or null if it doesn't

   private String verifyLogin(String username, String password) throws Exception {
        String phoneNumber = null;
		PreparedStatement selectStatement = sDBConnection.prepareStatement(SELECT_USERNAME_PASSWORD);
		selectStatement.setString(1, username);
		selectStatement.setString(2, password);
		System.out.println("select statement = " + selectStatement.toString());
		ResultSet rs = selectStatement.executeQuery();
		if (rs.first()) {
			int clientUserIdColIndex = rs.findColumn("PHONE_NUMBER");
			phoneNumber = rs.getString(clientUserIdColIndex);
			System.out.println("and the phone number is " + phoneNumber);
			rs.close();
			return phoneNumber;
		} 
        return null;
    }

	// Create the JSON payload including the terminal IP address and the traceroute to it.
	private String createPayload(String terminalIPAddress) throws Exception {
		return String.format("{\"%s\" : \"%s\"}", JSON_TAG_TERMINAL_IP_ADDRESS, terminalIPAddress);
	}

	private String tracerouteToJSON(String forwardTraceroute) {
	   	StringBuffer sbJSON = new StringBuffer();
		sbJSON.append("{");
	   	sbJSON.append(String.format("\"%s\" : [", JSON_TAG_TRACEROUTE));
		String[] forwardTracerouteArray = forwardTraceroute.split("\n");
		for (int i = 0; i < forwardTracerouteArray.length; i++) {
	   		String ipAddr = forwardTracerouteArray[i];
			sbJSON.append("\"" + ipAddr + "\"");
			if (i < forwardTracerouteArray.length - 1) {
				sbJSON.append(",");
			}
		}
		sbJSON.append("]}");
		return sbJSON.toString();
	}

	/**
	 * post the forward traceroute to the verfication URL.
	 * @param requestid unique identifier returned from the handset URL request to track additional information sent via login
	 * @param terminalIPAddress IP address of terminal which accessed this resource.
	 */
	private void postForwardTraceRoute(final String requestId, final String terminalIPAddress) throws Exception {
		Runnable tracerouteRunnable = new Runnable() {
			@Override
			public void run() {
				try {
					String forwardTraceroute = ProcessUtil.pipeCommand(String.format(TRACEROUTE_CMD, terminalIPAddress));
					System.out.println("forward traceroute : " + forwardTraceroute); 
					String tracerouteJSON = tracerouteToJSON(forwardTraceroute);
					String postTracerouteUrlStr = String.format(POST_TRACEROUTE_URL_FORMAT, Constants.getVerifyAddress(), requestId);
					URL postTracerouteUrl = new URL(postTracerouteUrlStr); 
					ServletUtil.postUrlString(postTracerouteUrl, tracerouteJSON);
				} catch (Exception ex) {
					System.out.println("error obtaining traceroute to terminal " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		};
		Thread thread = new Thread(tracerouteRunnable);
		thread.start();
	}

	private static JSONObject parseJSONFromStream(InputStream in) throws UnsupportedEncodingException, IOException {
		BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")); 
		StringBuilder responseStrBuilder = new StringBuilder();

		String inputStr;
		while ((inputStr = streamReader.readLine()) != null) {
			responseStrBuilder.append(inputStr);
		}
		return new JSONObject(responseStrBuilder.toString());
	}

	/**
	 * 1st response from the verify servlet
	 * Contains the non-NATTed IP address of the handset, token to match from redirected website, the
	 * requestId generated by the verify servlet, and the error message if any.
	 */
	private class HandsetURLAndToken {
		private String handsetURL;
		private String token;
		private String requestId;
		private String error;


		public HandsetURLAndToken(InputStream is) throws UnsupportedEncodingException, IOException {
			JSONObject jsonObject = parseJSONFromStream(is);
			error = jsonObject.getString("error");
			if (error.equals("SUCCESS")) {
				token = jsonObject.getString("token");
				handsetURL = jsonObject.getString("handsetURL");
				requestId = jsonObject.getString("requestId");
				System.out.println("HandsetInfo: error = " + error + " url = " + handsetURL + " requestId = " + requestId);
			} else {
				System.out.println("HandsetInfo: error = " + error + " requestId = " + requestId);
			}
		}

		// save the token from verification with the sessionID
		public void saveToken(Connection connection, String sessionId) throws Exception {
			PreparedStatement updateStatement = connection.prepareStatement(UPDATE_TOKEN_AND_SESSION_ID);
			updateStatement.setString(1, token);
			updateStatement.setString(2, requestId);
			updateStatement.setString(3, sessionId);
			updateStatement.execute();
		}

		public String getError() {
			return error;
		}

		public String getToken() {
			return token;
		}

		public String getHandsetURL() {
			return handsetURL;
		}

		public String getRequestId() {
			return requestId;
		}
	}
}
