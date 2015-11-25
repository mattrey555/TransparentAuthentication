package org.example.login;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
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
import java.util.List;
import java.util.Properties;
import com.visibleautomation.util.StringUtil;
import com.visibleautomation.util.ProcessUtil;

/**
 * Servlet implementation to process the login and request verification from the handset
 */
public class ProcessLoginServlet extends HttpServlet {
	private static final String SQL_USER = "third_party";
	private static final String SQL_PASSWORD = "FiatX1/9";
	private static final String SELECT_USERNAME_PASSWORD = "SELECT * FROM USER WHERE USER_ID=? AND PWD=?";
	private static final String VERIFY_URL_FORMAT = "http://104.154.58.18:8080/verify/verify?phoneNumber=%s&terminalIPAddress=%s&siteIPAddress=%s&clientId=%d&maxHops=%d&timeoutMsec=%d";
	private static final String IP_REGEXP = "\"[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*\"";
	private static final String TRACEROUTE_CMD = "traceroute -n --module=udp --queries=1 %s -w 0.25 --back | grep -v \"\\*\" | grep -o " + IP_REGEXP;
	private static final String JSON_TAG_TERMINAL_IP_ADDRESS = "terminalIPAddress";
	private static final String JSON_TAG_TRACEROUTE = "traceroute";
	private static final int CLIENT_ID = 1;
	private static final int MAX_HOPS = 20;
	private static final int TIMEOUT_MSEC = 20000;
	private static final String SQL_PROPERTIES_FILE = "sql.properties";
	private static final String SQL_USERNAME_PROPERTY = "sql.username";
	private static final String SQL_PASSWORD_PROPERTY = "sql.password";
	private static final String SQL_DATABASE_PROPERTY = "sql.database";
	private static final String URL_PARAM_USERNAME = "username";
	private static final String URL_PARAM_PASSWORD = "password";

    private static String sdbUsername = null;
	private static String sdbPassword = null;
	private static String sdbDatabase = null;
	
	static {
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

    public ProcessLoginServlet() {
		System.out.println("ProcessLoginServlet: constructor  called");
    }

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {

		try {	    
			String username  = request.getParameter(URL_PARAM_USERNAME);
			String password  = request.getParameter(URL_PARAM_PASSWORD);
			System.out.println("username = " + username + " password = " + password);
			String userId = verifyLogin(username, password);
			if (userId == null) {
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
				String verifyURLStr = String.format(VERIFY_URL_FORMAT, userId, terminalIPAddress, siteIPAddress, CLIENT_ID, MAX_HOPS, TIMEOUT_MSEC);
				URL verifyURL = new URL(verifyURLStr);
				HttpURLConnection verifyServletConnection = (HttpURLConnection) verifyURL.openConnection();
				verifyServletConnection.setRequestMethod("POST");
				verifyServletConnection.setDoOutput(true);
				String jsonPayload = createPayload(terminalIPAddress);
				System.out.println("writing payload " + jsonPayload);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(verifyServletConnection.getOutputStream()));
				bw.write(jsonPayload);
				bw.flush();
				verifyServletConnection.getOutputStream().close();

				System.out.println("user id = " + userId);
				InputStream is = verifyServletConnection.getInputStream();
          		BufferedReader br = new BufferedReader(new InputStreamReader(verifyServletConnection.getInputStream()));
				PrintWriter out = response.getWriter();
				String line = null;
            	while ((line = br.readLine()) != null) {
					out.println(line);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// given the username and password from the UI, return the userID if it exists, or null if it doesn't

   private String verifyLogin(String username, String password) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
        String userId = null;
		String dbConnection = String.format(Constants.DB_CONNECTION, sdbDatabase);
        Connection con = DriverManager.getConnection(dbConnection, sdbUsername, sdbPassword);
        try {
            PreparedStatement selectStatement = con.prepareStatement(SELECT_USERNAME_PASSWORD);
            selectStatement.setString(1, username);
            selectStatement.setString(2, password);
			System.out.println("select statement = " + selectStatement.toString());
            ResultSet rs = selectStatement.executeQuery();
            if (rs.first()) {
                int clientUserIdColIndex = rs.findColumn("CLIENT_USER_ID");
                userId = rs.getString(clientUserIdColIndex);
                System.out.println("and the user id is " + userId);
                rs.close();
				return userId;
            } 
        } finally {
            con.close();
        }
        return null;
    }

	// Create the JSON payload including the terminal IP address and the traceroute to it.
	private String createPayload(String terminalIPAddress) throws Exception {
	   	String forwardTraceroute = ProcessUtil.pipeCommand(String.format(TRACEROUTE_CMD, terminalIPAddress));
	   	System.out.println("forward traceroute : " + forwardTraceroute); 
   	   	List<String> forwardTracerouteList = StringUtil.splitList(forwardTraceroute, "\n");
	   	StringBuffer sbJSON = new StringBuffer();
		sbJSON.append("{");
		sbJSON.append(String.format("\"%s\" : \"%s\",\n", JSON_TAG_TERMINAL_IP_ADDRESS, terminalIPAddress));
	   	sbJSON.append(String.format("\"%s\" : [", JSON_TAG_TRACEROUTE));
		for (int i = 0; i < forwardTracerouteList.size(); i++) {
	   		String ipAddr = forwardTracerouteList.get(i);
			sbJSON.append("\"" + ipAddr + "\"");
			if (i < forwardTracerouteList.size() - 1) {
				sbJSON.append(",");
			}
		}
		sbJSON.append("]}");
		return sbJSON.toString();
	}
															    
}
