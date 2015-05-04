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
import java.util.List;
import java.util.ArrayList;
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
import com.visibleautomation.util.ServletUtil;
import com.visibleautomation.util.StringUtil;
import com.visibleautomation.util.ProcessUtil;

// rad the responses from the handset.
// there are two responses: the Wifi MAC address (bssid) and location
// and the reverse traceroute from the handset

public class Respond extends HttpServlet {
	private static final String REQUEST_ID = "requestId"; 
	private static final String MAC_ADDRESS = "macAddress"; 
	private static final String LATITUDE = "latitude"; 
	private static final String LONGITUDE = "longitude"; 
	private static final String TRACEROUTE = "traceroute"; 
	private static final String REQUEST_TABLE = "request";
	private static final String SQL_USER = "verify";
	private static final String SQL_PASSWORD = "FiatX1/9";
	private static final int SUCCESS = 0;
	private static final int BAD_URL = 1;
	private static final int USER_NOT_FOUND = 2;
	private static final int DATABASE_CONNECTION_FAILED = 2;
	private static final String UPDATE_LOCATION = "UPDATE request SET latitude=?,longitude=?,handset_ip_address=? WHERE request_id=?";
	private static final String UPDATE_LOCATION_MACADDRESS = "UPDATE request set latitude=?,longitude=?,wifi_mac_addr=?,handset_ip_address=? WHERE request_id=?";
	private static final String UPDATE_MACADDRESS = "UPDATE REQUEST SET WIFI_MAC_ADDR=?,HANDSET_IP_ADDRESS=? WHERE REQUEST_ID=?";
	private static final String TRACEROUTE_CMD = "traceroute -n --module=udp --queries=1 %s -w 0.25 --back | grep -v \"\\*\" | grep -o \"[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*\"";
	private static final String JSON_TAG_LATITUDE = "latitude";
	private static final String JSON_TAG_LONGITUDE = "longitude";
	private static final String JSON_TAG_BSSSID = "bssid";
	private static final double BAD_LOCATION = -500.0;

	public Respond() {
		System.out.println("Respond: constructor");
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
		String requestId = null; 
		String ipAddressStr = null;
		boolean traceroute = false;
		int maxHops = 16;
		int errorCode = SUCCESS;
		String errorMessage = "SUCCESS";
		String ipAddress = request.getRemoteAddr();
		if (request.getQueryString() != null) {
			String[] pairs = request.getQueryString().split("&");
			try {   
				for (String pair : pairs) {
					String param = getParam(pair); 
					String value = getValue(pair); 
					if (param.equals(REQUEST_ID)) {
						requestId = value;
					}               
					if (param.equals(TRACEROUTE)) {
						traceroute = Boolean.valueOf(value);
					}               
				}           
			} catch (Exception ex) {
				errorMessage = ex.getMessage();
				errorCode = BAD_URL;
			}
		} else {
			errorCode = BAD_URL;
		}
		System.out.println("parameters parsed");
		System.out.println("requestId = " + requestId);
		System.out.println("traceroute = " + traceroute);
		try {
			RequestSet requestSet = RequestSet.getInstance();
			ResponseData responseData = requestSet.getResponseData(requestId);
			if (responseData == null) {
				System.out.println("error: no response data for " + requestId);
			} else {
				String postData = ServletUtil.readPostData(request);
				System.out.println("post data = " + postData);
				if (requestId != null) {
					if (!traceroute) {
						JSONObject jsonObject = new JSONObject(postData);
						System.out.println("JSON data = " + jsonObject);
						double latitude  = jsonObject.optDouble(JSON_TAG_LATITUDE, BAD_LOCATION);
						double longitude  = jsonObject.optDouble(JSON_TAG_LONGITUDE, BAD_LOCATION);
						String macAddress = jsonObject.getString("bssid");

						if ((longitude != BAD_LOCATION) && (latitude != BAD_LOCATION)) {
							responseData.handsetLatitude = latitude;
							responseData.handsetLongitude = longitude;
							responseData.handsetIPAddress = ipAddress;
							if (macAddress != null) {
								responseData.wifiMACAddress = macAddress;
								updateLocationMacAddress(requestId, macAddress, latitude, longitude, ipAddress);
							} else {
								updateLocation(requestId, latitude, longitude, ipAddress);
							}
						} else {
							updateMacAddress(requestId, macAddress, ipAddress);
						}
						String forwardTraceroute = ProcessUtil.pipeCommand(String.format(TRACEROUTE_CMD, ipAddress));
						System.out.println("forward traceroute : " + forwardTraceroute);
						responseData.forwardTraceroute = StringUtil.splitList(forwardTraceroute, "\n");
					} else {
						// parse GSON POST data.
						JSONArray jsonArray = new JSONArray(postData);
						List<String> reverseTraceroute = new ArrayList<String>(jsonArray.length());
						for (int i = 0; i < jsonArray.length(); i++) {
							reverseTraceroute.add((String) jsonArray.get(i));
						}
						responseData.reverseTraceroute = reverseTraceroute;
						requestSet.notify(requestId);
					}
				}
			}
		} catch (Exception ex) {
		    ex.printStackTrace();
		}
	}

	// update the user's location and MAC address in the database
	private void updateLocationMacAddress(String requestId, String macAddress, double latitude, double longitude, String ipAddress) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		Connection con = DriverManager.getConnection(Constants.DB_CONNECTION, SQL_USER, SQL_PASSWORD);
		try {
			PreparedStatement updateStatement = con.prepareStatement(UPDATE_LOCATION_MACADDRESS);
			updateStatement.setDouble(1, latitude);
			updateStatement.setDouble(2, longitude);
			updateStatement.setString(3, macAddress);
			updateStatement.setString(4, ipAddress);
			updateStatement.setString(5, requestId);
			updateStatement.execute();
		} finally {
			con.close();
		}
	}

    // update the user's location in the database
	private void updateLocation(String requestId, double latitude, double longitude, String ipAddress) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		Connection con = DriverManager.getConnection(Constants.DB_CONNECTION, SQL_USER, SQL_PASSWORD);
		try {
			PreparedStatement updateStatement = con.prepareStatement(UPDATE_LOCATION);
			updateStatement.setDouble(1, latitude);
			updateStatement.setDouble(2, longitude);
			updateStatement.setString(3, ipAddress);
			updateStatement.setString(4, requestId);
			updateStatement.execute();
		} finally {
			con.close();
		}
	}

	// update the user's connected Wifi MAC address in the daabase
	private void updateMacAddress(String requestId, String macAddress, String ipAddress) throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		Connection con = DriverManager.getConnection(Constants.DB_CONNECTION, SQL_USER, SQL_PASSWORD);
		try {
			PreparedStatement updateStatement = con.prepareStatement(UPDATE_LOCATION);
			updateStatement.setString(1, macAddress);
			updateStatement.setString(2, ipAddress);
			updateStatement.setString(3, requestId);
			updateStatement.execute();
		} finally {
			con.close();
		}
	}

	private static String getParam(String pair) throws UnsupportedEncodingException {
        int idx = pair.indexOf("=");
        return URLDecoder.decode(pair.substring(0, idx), "UTF-8");
	}

	private static String getValue(String pair) throws UnsupportedEncodingException {
        int idx = pair.indexOf("=");
        return URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
	}
  
	public void destroy() {
      // do nothing.
	}


}
