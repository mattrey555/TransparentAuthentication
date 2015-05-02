package com.visibleautomation.verify;
import java.io.IOException;
import java.io.*;
import javax.servlet.*;
import java.net.URL;
import java.lang.Integer;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.net.InetAddress;
import javax.servlet.http.*;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.json.JSONArray;
import org.json.JSONObject;
import com.visibleautomation.xmpp.SmackCcsClient;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;
import com.visibleautomation.util.ServletUtil;

public class Verify extends HttpServlet {
	private static final String PHONE_NUMBER = "phoneNumber"; 
	private static final String CLIENT_ID = "clientId"; 
	private static final String MAX_HOPS = "maxHops"; 
	private static final String SITE_IPADDRESS = "siteIPAddress"; 
	private static final String TERMINAL_IPADDRESS = "terminalIPAddress"; 
	private static final String TIMEOUT_MSEC = "timeoutMsec"; 
	private static final String REGISTRATION_ID = "registration_id";
	private static final String REQUEST_TABLE = "request";
	private static final String SQL_USER = "verify";
	private static final String SQL_PASSWORD = "FiatX1/9";
	private static final int SUCCESS = 0;
	private static final int BAD_URL = 1;
	private static final int USER_NOT_FOUND = 2;
	private static final int DATABASE_CONNECTION_FAILED = 2;
	private static final int POLL_MSEC = 100;
    private static final String SELECT_BY_PHONE = "select * from user where phone_number=?";
	private static final String INSERT_REQUEST_ID = "INSERT INTO request (request_id, request_timestamp) VALUES (?, ?)"; 
	private static String mVerificationResult;


	public Verify() {
		System.out.println("constructor is called");
  	}

	public void init() throws ServletException {
      	// Do required initialization
      	System.out.println("init is getting called");
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      	// Set response content type
      	String urlStr = request.getRequestURL().toString();
      	URL url = new URL(urlStr); 
      	response.setContentType("application/json");
		System.out.println("doGet is getting called query = " + request.getQueryString());
		String phoneNumber = null; 
		String clientId = null; 
		String errorMessage = "Success";
		String terminalIPAddress = null;
		String siteIPAddress = null;
		int maxHops = 16;
		int errorCode = SUCCESS;
		int timeoutMsec = 20000;
		if (request.getQueryString() != null) {
			String[] pairs = request.getQueryString().split("&");
			try {   
				for (String pair : pairs) {
					String param = getParam(pair); 
					String value = getValue(pair); 
					System.out.println("param = " + param + " value = " + value);
					if (param.equals(PHONE_NUMBER)) {
						phoneNumber = value;
					}              
					if (param.equals(CLIENT_ID)) {
						clientId = value;
					}               
					if (param.equals(SITE_IPADDRESS)) {
						siteIPAddress = value;
					}               
					if (param.equals(TERMINAL_IPADDRESS)) {
						terminalIPAddress = value;
					}               
					if (param.equals(MAX_HOPS)) {
						maxHops = Integer.valueOf(value);
					}               
					if (param.equals(TIMEOUT_MSEC)) {
						timeoutMsec = Integer.valueOf(value);
					}               
				}           
			} catch (Exception ex) {
				errorMessage = ex.getMessage();
				errorCode = BAD_URL;
			}
		} else {
			errorCode = BAD_URL;
		}
		String requestId = UUID.randomUUID().toString();
		System.out.println("parameters parsed");
		System.out.println("phoneNumber = " + phoneNumber);
		System.out.println("clientId = " + clientId);
		System.out.println("siteIPAddress = " + siteIPAddress);
		System.out.println("terminalIPAddress = " + terminalIPAddress);
		System.out.println("maxHops = " + maxHops);
		System.out.println("timeoutMsec = " + timeoutMsec);
		System.out.println("requestId = " + requestId);
        String postData = ServletUtil.readPostData(request);
        System.out.println("post data = " + postData);
		JSONObject jsonObject = new JSONObject(postData);
		String terminalIPAddressFromJSON = jsonObject.getString("terminalIPAddress");
		JSONArray terminalTracerouteJSON = jsonObject.getJSONArray("traceroute");
		List<String> terminalTraceroute = new ArrayList<String>(terminalTracerouteJSON.length());
		for (int i = 0; i < terminalTracerouteJSON.length(); i++) {
			terminalTraceroute.add(terminalTracerouteJSON.getString(i));
		}
		try {
			if ((phoneNumber != null) && (clientId != null)) {
				String userId = getGCMessagingID(phoneNumber);
				System.out.println("sending message to " + userId);
				if (userId != null) {

					// add a request with a Unique # and add it to the outstanding request list.
					RequestSet requestSet = RequestSet.getInstance();
					requestSet.addRequest(requestId, new ResponseData());
					initRequest(requestId);

					// get this server's IP address
					String serverAddress = url.getHost();
					InetAddress serverInetAddress = InetAddress.getByName(url.getHost());
					String serverIP = serverInetAddress.getHostAddress();
					int serverPort = url.getPort();

					// request a verification from the handset
					verifyIPAddress(userId, siteIPAddress, maxHops, timeoutMsec, requestId, serverIP, serverPort);

					// wait on the first response from the handset with the location and Connected MAC address
					ResponseData responseData = requestSet.waitOnResponse(requestId, timeoutMsec);
					responseData.terminalTraceroute = terminalTraceroute;
					PrintWriter out = response.getWriter();
					out.println("longitude = " + responseData.longitude);
					out.println("latitude = " + responseData.latitude);
					out.println("handsetIPAddress = " + responseData.handsetIPAddress);
					out.println("wifiMACAddress = " + responseData.wifiMACAddress);
					if (responseData.forwardTraceroute != null) {
						out.println("forward traceroute");
						for (String ip : responseData.forwardTraceroute) {
							out.println(ip + " ");
						}
					}
					if (responseData.reverseTraceroute != null) {
						out.println("reverse traceroute");
						for (String ip : responseData.reverseTraceroute) {
							out.println(ip + " ");
						}
					}
					if (responseData.terminalTraceroute != null) {
						out.println("forward traceroute to terminal");
						for (String ip : responseData.terminalTraceroute) {
							out.println(ip + " ");
						}
					}
					requestSet.removeRequest(requestId);
				} else {
					errorCode = USER_NOT_FOUND;
				}
			}
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	}


	private String getGCMessagingID(String phoneNumber) throws Exception {
		String userId = null;
		Class.forName("com.mysql.jdbc.Driver");
		Connection con = DriverManager.getConnection(Constants.DB_CONNECTION, SQL_USER, SQL_PASSWORD);
		try {
			PreparedStatement selectStatement = con.prepareStatement(SELECT_BY_PHONE);
			selectStatement.setString(1, phoneNumber);
			ResultSet rs = selectStatement.executeQuery();
			if (rs.first()) {
				System.out.println("there is a matching record for " + phoneNumber);
				int phoneNumberColIndex = rs.findColumn("phone_number");
				String testPhoneNumber = rs.getString(phoneNumberColIndex);
				int clientUserIdColIndex = rs.findColumn("client_user_id");
				userId = rs.getString(clientUserIdColIndex);
				System.out.println("and the user id is " + userId);
				rs.close();
			} else {
				System.out.println("there was no user matching " + phoneNumber);
			}
		} finally {
			con.close();
		}
		return userId;
	}

	public void initRequest(String requestId) throws Exception {
		long timestamp = new Date().getTime();
		Class.forName("com.mysql.jdbc.Driver");
		Connection con = DriverManager.getConnection(Constants.DB_CONNECTION, SQL_USER, SQL_PASSWORD);
		try {
			PreparedStatement insertStatement = con.prepareStatement(INSERT_REQUEST_ID);
			insertStatement.setString(1, requestId);
			insertStatement.setLong(2, timestamp);
			insertStatement.execute();
		} finally {
			con.close();
		}
	}

	public String getParam(String pair) throws UnsupportedEncodingException {
        int idx = pair.indexOf("=");
        return URLDecoder.decode(pair.substring(0, idx), "UTF-8");
	}

	public String getValue(String pair) throws UnsupportedEncodingException {
        int idx = pair.indexOf("=");
        return URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
	}
  
	public void destroy() {
      // do nothing.
	}

		/**
		 * Send an XMPP Push Notification requesting a verification from the handset
		 * toRegId GCM messaging ID.
		 * destIPAddress IP address to request a reverse traceroute to. (the site IPAddress)
		 * maxHops maximum # of hops to request on reverse traceroute
		 * timeoutMsec response timeout
		 * requestId requestID unique identifier, so we can associate the response from the handset.
		 * requestIp Address of this servlet to send the response to.
		 */
		public static void verifyIPAddress(final String toRegId, 
						   final String destIpAddress, 
						   final int maxHops, 
						   int timeoutMsec, 
						   final String requestId,
						   final String requestIp,
						   final int requestPort) {
			System.out.println("sending message to " + toRegId);
			final long senderId = 1012198772634L; // your GCM sender id
			final String password = "AIzaSyDUuok4W2TqMR9vKmkQd66Fm9j3SYxhHQo";
			mVerificationResult = null;
			try {
				SmackCcsClient ccsClient = new SmackCcsClient();

				ccsClient.connect(senderId, password);

				// Send a sample hello downstream message to a device.
				String messageId = ccsClient.nextMessageId();
				Map<String, String> payload = new HashMap<String, String>();
				payload.put("RequestAuthorization", "Request");
				payload.put("destIpAddress", destIpAddress);
				payload.put("requestId", requestId);
				payload.put("maxHops", Integer.toString(maxHops));
				payload.put("EmbeddedMessageId", messageId);
				payload.put("requestIp", requestIp);
				payload.put("requestPort", Integer.toString(requestPort));
				payload.put("delivery_receipt_requested", "true");
				String collapseKey = "sample";
				Long timeToLive = 20000L;
				String message = createJsonMessage(toRegId, messageId, payload, collapseKey, timeToLive, true);
				System.out.println("sending " + message);
				ccsClient.sendDownstreamMessage(message);
			} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

    /**
     * Creates a JSON encoded GCM message.
     *
     * @param to RegistrationId of the target device (Required).
     * @param messageId Unique messageId for which CCS will send an
     *         "ack/nack" (Required).
     * @param payload Message content intended for the application. (Optional).
     * @param collapseKey GCM collapse_key parameter (Optional).
     * @param timeToLive GCM time_to_live parameter (Optional).
     * @param delayWhileIdle GCM delay_while_idle parameter (Optional).
     * @return JSON encoded GCM message.
     */
    public static String createJsonMessage(String to, String messageId,
            Map<String, String> payload, String collapseKey, Long timeToLive,
            Boolean delayWhileIdle) {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("to", to);
        if (collapseKey != null) {
            message.put("collapse_key", collapseKey);
        }
        if (timeToLive != null) {
            message.put("time_to_live", timeToLive);
        }
        if (delayWhileIdle != null && delayWhileIdle) {
            message.put("delay_while_idle", true);
        }
      message.put("message_id", messageId);
      message.put("data", payload);
      return JSONValue.toJSONString(message);
    }
} 
