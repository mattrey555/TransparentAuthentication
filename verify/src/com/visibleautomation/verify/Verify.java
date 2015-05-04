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
import java.lang.Thread;
import java.lang.Runnable;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.URL;
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
import com.visibleautomation.util.StreamUtil;

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
	private static final String REQUEST_IP_LOCATION = "http://ipinfo.io/%s/json";
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
					ResponseData responseData = new ResponseData();
					requestSet.addRequest(requestId, responseData);
					responseData.terminalIPAddress = terminalIPAddress;
					initRequest(requestId);
					getTerminalLocationInBackground(responseData);

					// get this server's IP address
					String serverAddress = url.getHost();
					InetAddress serverInetAddress = InetAddress.getByName(url.getHost());
					String serverIP = serverInetAddress.getHostAddress();
					int serverPort = url.getPort();

					// request a verification from the handset
					verifyIPAddress(userId, siteIPAddress, maxHops, timeoutMsec, requestId, serverIP, serverPort);

					// wait on the first response from the handset with the location and Connected MAC address
					responseData = requestSet.waitOnResponse(requestId, timeoutMsec);
					responseData.terminalTraceroute = terminalTraceroute;
					PrintWriter out = response.getWriter();
					out.println("longitude = " + responseData.handsetLongitude);
					out.println("latitude = " + responseData.handsetLatitude);
					out.println("handsetIPAddress = " + responseData.handsetIPAddress);
					out.println("wifiMACAddress = " + responseData.wifiMACAddress);
					out.println(String.format("terminal location %.4f %.4f", responseData.terminalLatitude, responseData.terminalLongitude));
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

	// retreive the Google Cloud Messaging ID for the user's phone number
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

	// initialize the request by storing the request ID and the timestamp.
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

	// get the value for the spectified parameter name
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
				String message = createGCMMessage(toRegId, messageId, payload, collapseKey, timeToLive, true);
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
    public static String createGCMMessage(String to, String messageId,
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

    /**
     * parse the latitude longitude from ipinfo.io {
  	 * "ip": "24.17.219.54",
     * "hostname": "c-24-17-219-54.hsd1.wa.comcast.net",
     * "city": "Bellevue",
     * "region": "Washington",
     * "country": "US",
     * "loc": "47.6201,-122.1408",
     * "org": "AS7922 Comcast Cable Communications, Inc.",
     * "postal": "98007"
     * }
     */
    public static void getTerminalLocation(ResponseData responseData) throws IOException {
		URL locationURL = new URL(String.format(REQUEST_IP_LOCATION, responseData.terminalIPAddress));
		HttpURLConnection conn = (HttpURLConnection)  locationURL.openConnection();
		InputStream is = conn.getInputStream();
		String responseStr = StreamUtil.readToString(is);
		JSONObject jsonObject = new JSONObject(responseStr);
		String locationStr = jsonObject.getString("loc");
		String[] latlon = locationStr.split(",");
		responseData.terminalLatitude = Double.parseDouble(latlon[0]);
		responseData.terminalLongitude = Double.parseDouble(latlon[1]);


	}

	public static void getTerminalLocationInBackground(final ResponseData responseData) {
		Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					getTerminalLocation(responseData);
				} catch (IOException ioex) {
					System.out.println("unable to read data from location service for terminal");
					ioex.printStackTrace();
				}
			}
		});
		thread.start();
	}
} 
