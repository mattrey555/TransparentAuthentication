package com.visibleautomation.verify;
import java.io.IOException;
import java.io.*;
import javax.servlet.*;
import java.net.URL;
import java.lang.Integer;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
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
import org.json.JSONObject;
import org.json.JSONArray;
import com.visibleautomation.xmpp.SmackCcsClient;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.StanzaListener;
import com.visibleautomation.util.ServletUtil;
import com.visibleautomation.util.StreamUtil;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Servlet which receives verification request from the third-party server.
 */
public class Verify extends HttpServlet {
	private static final int SUCCESS = 0;
	private static final int BAD_URL = 1;
	private static final int USER_NOT_FOUND = 2;
	private static final int POLL_MSEC = 100;
	private static final int XMPP_TIMEOUT_MSEC = 20000;
	private static final long DEFAULT_TIME_TO_LIVE = 20000L;
	private static final String INSERT_REQUEST_ID = "INSERT INTO request (request_id, message_id, request_timestamp, request_token, terminal_ip_address, user_agent) VALUES (?, ?, ?, ?, ?, ?)"; 
	private static final String REQUEST_IP_LOCATION = "http://ipinfo.io/%s/json";
	private static final String SUCCESS_JSON = "{ \"error\": \"SUCCESS\", \"token\": \"%s\", \"handsetURL\": \"%s\", \"requestId\": \"%s\"}";
	private static final String ERROR_JSON = "{ \"error\": \"%s\", \"requestId\": \"%s\"}";
	private static final String ERROR_NO_MOBILE_RESPONSE_VALUE = "FAILED_CONTACT_MOBILE";
	private static final String ERROR_MOBILE_NOT_ON_WIFI_VALUE = "FAILED_MOBILE_NOT_ON_WIFI";
	private static final String ERROR_MOBILE_EXCEPTION = "FAILED_MOBILE_EXCEPTION";
	private static final String FROM = "from";
	private static String mVerificationResult;
	private static SmackCcsClient.XMPPRunnable mXMPPRunnable;
    private static boolean mCcsClientConnected = false;
	private static Connection sDBConnection;
	private static Logger logger;

	/**
     * static initialization: load the properties into constants, load the JDBC devier, and open the connection to the database.
     */
    static {
	    logger = LogManager.getLogger(SmackCcsClient.class);
	    logger.debug("initialization");
		Constants.setDatabaseVariables();
		Constants.setGCMVariables();
		try {
			Class.forName("com.mysql.jdbc.Driver");
			String dbConnection = String.format(Constants.DB_CONNECTION_FORMAT, Constants.getDBDatabase());
			sDBConnection = DriverManager.getConnection(dbConnection, Constants.getDBUsername(), Constants.getDBPassword());
		} catch (Exception ex) {
			logger.debug("failed to initialize database " + ex.getMessage());
		}
    }

	/**
     * standard servlet post method
     */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
      	// Set response content type
      	response.setContentType("application/json");
		logger.debug("doGet is getting called query = " + request.getQueryString());

		// parse the URL and post data into a VerifyRequest object
		VerifyRequest verifyRequest = null; 
		String errorMessage = null;
		int errorCode = SUCCESS;
		try {
			String postData = ServletUtil.readPostData(request);
			verifyRequest = new VerifyRequest(request.getQueryString(), postData);
			verifyRequest.log();
		} catch (Exception ex) {
			errorMessage = ex.getMessage();
			errorCode = BAD_URL;
		}

		// generate a random # to send to the mobile (TODO: make this SecureRandom()
        Random random = new Random();
        long messageId = random.nextLong();

		try {
			if ((verifyRequest != null) && (verifyRequest.getPhoneNumber() != null) && (verifyRequest.getClientId() != null)) {
			    ClientData clientData = new ClientData(sDBConnection, verifyRequest.getPhoneNumber());
				logger.debug("sending message to " + clientData.getGCMMessagingId());

				// add a request with a Unique # and add it to the outstanding request list.
				/// ResponseData responseData = new ResponseData();
				// responseData.terminalIPAddress = verifyRequest.getTerminalIPAddress();
				// getTerminalLocationInBackground(responseData);
				// request a verification from the handset
			    CipherUtil.Token token = CipherUtil.getEncryptedToken(clientData.getPublicKey());
				MobileResponseCallback responseCallback  = sendTokenAndWaitForResponse(clientData, verifyRequest, messageId, token);
				PrintWriter out = response.getWriter();
				if (responseCallback.getError() != null) {
					logger.debug("returning failure error " + responseCallback.getError());
					out.println(String.format(ERROR_JSON, responseCallback.getError(), verifyRequest.getRequestId()));
				} else if (responseCallback.getHandsetURL() != null) {
					logger.debug("returning success message");
					out.println(String.format(SUCCESS_JSON, token.getToken(), responseCallback.getHandsetURL(), verifyRequest.getRequestId()));
				} else if (responseCallback.hasExpired()) {
					logger.debug("returning failure no response form mobile");
					out.println(String.format(ERROR_JSON, ERROR_NO_MOBILE_RESPONSE_VALUE,  verifyRequest.getRequestId()));
				} else if (responseCallback.isCellular()) {
					logger.debug("returning failure no response form mobile");
					out.println(String.format(ERROR_JSON, ERROR_MOBILE_NOT_ON_WIFI_VALUE, verifyRequest.getRequestId()));
				}

			}
		} catch (Exception ex) {
			logger.error("registration failed", ex);
		}
	}

	/**
	 * callback to wait for the Handset URL which is forwarded to the 3rd party to get the token from.
	 */
	private class MobileResponseCallback implements SmackCcsClient.GcmStanzaCallback {
		private static final String EXTRA_HANDSET_ADDRESS = "handsetIpAddress";
		private static final String EXTRA_NETWORK = "network";
		private static final String VALUE_CELLULAR = "cellular";
		private static final String EXTRA_DATA = "data";
		private static final String EXTRA_ERROR = "error";
		private String handsetURL = null;
		private boolean cellular = false;
		private boolean expired = false;
		private String error = null;

		/**
		 * wait for the "data" JSON object to come back from the handset
		 */
	   	public void run(Map<String, Object> jsonObject) {
		    org.json.simple.JSONObject jsonDataObject = (org.json.simple.JSONObject) jsonObject.get(EXTRA_DATA);
			for (Object key : jsonDataObject.keySet()) {
				logger.debug("data key: " + key);
				if (jsonDataObject.get(key) != null) {
					logger.debug("data type: " + jsonDataObject.get(key).getClass());
				} else {
					logger.debug("data was null");
				}
			}

			// if the handset was connected to the cellular network, we get {"network" : "cellular"} in the response
			// and we want to send that back to the third party so they can inform the user to switch over to Wifi
			// for verification.
			// for errors, we get: {"error":"DenyAuthorization"}, or other errors
			String network = (String) jsonDataObject.get(EXTRA_NETWORK);
			error = (String) jsonDataObject.get(EXTRA_ERROR);
			if (error != null) {
				logger.debug("error = " + error);
				mXMPPRunnable.getCcsClient().getConnection().removeAssociatedStanzaListener(this);
			} else if ((network == null) || !network.equals(VALUE_CELLULAR)) {
	   			handsetURL = (String) jsonDataObject.get(EXTRA_HANDSET_ADDRESS);	
				if (handsetURL != null) {
					logger.debug("handsetURL = " + handsetURL);
					mXMPPRunnable.getCcsClient().getConnection().removeAssociatedStanzaListener(this);
				}
			} else {
				logger.debug("device is connected to cellular network, not WiFI");
				mXMPPRunnable.getCcsClient().getConnection().removeAssociatedStanzaListener(this);
				cellular = true;
			}
	   	}

	   	public void expired() {
			expired = true;
	   	}

		public boolean hasExpired() {
			return expired;
		}

		public String getHandsetURL() {
			return handsetURL;
		}

		public boolean isCellular() {
			return cellular;
		}

		public String getError() {
			return error;
		}
	}



	// initialize the request by storing the request ID and the timestamp.
	public static void saveRequestToDatabase(String requestId, String terminalIPAddress, String userAgent, long messageId, long token) throws Exception {
		long timestamp = new Date().getTime();
		PreparedStatement insertStatement = sDBConnection.prepareStatement(INSERT_REQUEST_ID);
		insertStatement.setString(1, requestId);
		insertStatement.setLong(2, messageId);
		insertStatement.setLong(3, timestamp);
		insertStatement.setLong(4, token);
		insertStatement.setString(5, terminalIPAddress);
		insertStatement.setString(6, userAgent);
		insertStatement.execute();
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
     * messageId unique identifier to associate response from handset (note: may be duplicate)
	 * requestIp Address of this servlet to send the response to.
	 */
	public MobileResponseCallback sendTokenAndWaitForResponse(ClientData clientData,
															  final VerifyRequest verifyRequest,
															  final long messageId,
															  CipherUtil.Token token) {
		logger.debug("sending message to " + clientData.getGCMMessagingId());
		mVerificationResult = null;
		try {
			
			// maintain a continuous connection to the XMPP service, and install a thread which listens to messages.
			synchronized(this) {
				if (!mCcsClientConnected) {
					mXMPPRunnable = new SmackCcsClient.XMPPRunnable(Constants.getGCMProjectId(), Constants.getGCMApiKey(), 
																	Constants.getGCMServer(), Constants.getGCMPort());
					Thread xmppThread = new Thread(mXMPPRunnable);
					xmppThread.start();
					mCcsClientConnected = true;
				}
			}
			// Send a sample hello downstream message to a device.
			String ccsMessageId = mXMPPRunnable.getCcsClient().nextMessageId();
			Map<String, String> payload = new HashMap<String, String>();
			payload.put("RequestAuthorization", "Request");
			payload.put("destIpAddress", verifyRequest.getSiteIPAddress());
			payload.put("requestId", verifyRequest.getRequestId());
			payload.put("messageId", ccsMessageId);
			payload.put("maxHops", Integer.toString(verifyRequest.getMaxHops()));
			payload.put("embeddedMessageId", Long.toString(messageId));
			payload.put("delivery_receipt_requested", "true");
			payload.put("token", token.getEncryptedToken());
			if (verifyRequest.getClientMessage() != null) {
				payload.put("client_message", verifyRequest.getClientMessage());
			}

			// what should this be?
			String collapseKey = UUID.randomUUID().toString();
			MobileResponseCallback callback = new MobileResponseCallback();
			mXMPPRunnable.getCcsClient().addStanzaCallback(FROM, clientData.getGCMMessagingId(), Constants.getGCMProjectId(),
														   callback, verifyRequest.getTimeoutMsec());
			Long timeToLive = DEFAULT_TIME_TO_LIVE;
			String message = createGCMMessage(clientData.getGCMMessagingId(), ccsMessageId, payload, collapseKey, timeToLive, false);
			logger.debug("sending " + message);
			mXMPPRunnable.getCcsClient().sendDownstreamMessage(message);
			saveRequestToDatabase(verifyRequest.getRequestId(), verifyRequest.getTerminalIPAddress(), verifyRequest.getUserAgent(), messageId, token.getToken());
			waitForHandsetResponse(callback);
			return callback;
		} catch (Exception ex) {
			logger.error("error waiting for response from handset", ex);
			return null;
		}
	}

	/**
	 * The XMPPRunnable is running in the background, and we're waiting for a response from the handset.
	 */
	public boolean waitForHandsetResponse(MobileResponseCallback callback) {
		while ((callback.getHandsetURL() == null) && !callback.isCellular() && !callback.hasExpired() && (callback.getError() == null)) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException iex) {
				break;
			}
		}
		return (callback.getHandsetURL() != null) || callback.isCellular() || (callback.getError() != null);
	}

    /**
     * Creates a JSON encoded GCM message.
     * see https://developers.google.com/cloud-messaging/http-server-ref
     * @param to RegistrationId of the target device (Required).
     * @param messageId Unique messageId for which CCS will send an
     *         "ack/nack" (Required).
     * @param payload Message content intended for the application. (Optional).
     * @param collapseKey GCM collapse_key parameter (Optional).
     * @param timeToLive GCM time_to_live parameter (Optional).
     * @param delayWhileIdle GCM delay_while_idle parameter (Optional). Delay sending the message if
	 *		  the device is idle.
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
      	message.put("priority", "high");
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
					logger.debug("unable to read data from location service for terminal");
					ioex.printStackTrace();
				}
			}
		});
		thread.start();
	}
} 
