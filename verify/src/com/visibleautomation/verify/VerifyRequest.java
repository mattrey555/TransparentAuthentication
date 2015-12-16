package com.visibleautomation.verify;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import org.json.JSONObject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * parse the URL and JSON post data for a verification request
 */
public class VerifyRequest {
    // phone number used as a unique identifier, which is associated with the GCM client ID in the database
    private static final String PHONE_NUMBER = "phoneNumber"; 

    // third-party client ID
    private static final String CLIENT_ID = "clientId"; 

    // max # of hops to request in reverse traceroute
    private static final String MAX_HOPS = "maxHops"; 

    // third-party site address
    private static final String SITE_IPADDRESS = "siteIPAddress"; 

    // IP address of accessing terminal
    private static final String TERMINAL_IPADDRESS = "terminalIPAddress"; 

    // timeout to connect to client
    private static final String TIMEOUT_MSEC = "timeoutMsec"; 

    // callback URL for posting traceroute
    private static final String CALLBACK_URL = "callbackUrl"; 

	// JSON tag for the IP address of the terminal which accessed the website.
	private static final String JSON_TAG_TERMINAL_IP_ADDRESS = "terminalIPAddress";

	// JSON tag for custom client message
	private static final String JSON_TAG_CLIENT_MESSAGE = "clientMessage";

	// JSON tag user-agent of terminal requesting access.
	private static final String JSON_TAG_USER_AGENT = "userAgent";

    private String phoneNumber;
    private String clientId;
    private String terminalIPAddress;
    private String clientMessage;
    private String siteIPAddress;
    private String callbackUrl;
    private String requestId;
	private String userAgent;
    private int maxHops = 16;
    private int timeoutMsec = 20000;
	private Logger logger =  LogManager.getLogger(VerifyRequest.class);

    public VerifyRequest(String queryString, String postData) throws Exception {
		String[] pairs = queryString.split("&");
		for (String pair : pairs) {
			String param = getParam(pair); 
			String value = getValue(pair); 
			logger.debug("param = " + param + " value = " + value);
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
			if (param.equals(CALLBACK_URL)) {
				callbackUrl = value;
			}               
		}           

        // read the traceroute sent from the third-party website.
        // TODO: receive this in a separate request for performance.
        logger.debug("post data = " + postData);
        JSONObject jsonObject = new JSONObject(postData);
        terminalIPAddress = jsonObject.getString(JSON_TAG_TERMINAL_IP_ADDRESS);
		clientMessage = jsonObject.getString(JSON_TAG_CLIENT_MESSAGE);
		userAgent = jsonObject.getString(JSON_TAG_USER_AGENT);
		requestId = UUID.randomUUID().toString();
    }

    public String getPhoneNumber() {
		return phoneNumber;
    }

    public String getClientId() {
    	return clientId;
    }

    public String getRequestId() {
    	return requestId;
    }

    public String getSiteIPAddress() {
    	return siteIPAddress;
    }

    public String getTerminalIPAddress() {
    	return terminalIPAddress;
    }

    public int getMaxHops() {
    	return maxHops;
    }

    public int getTimeoutMsec() {
    	return timeoutMsec;
    }

    public String getCallbackUrl() {
    	return callbackUrl;
    }

	public String getClientMessage() {
		return clientMessage;
	}

	public String getUserAgent() {
		return userAgent;
	}


	public void log() {
        logger.debug("parameters parsed");
        logger.debug("phoneNumber = " + phoneNumber);
        logger.debug("clientId = " + clientId);
        logger.debug("siteIPAddress = " + siteIPAddress);
        logger.debug("terminalIPAddress = " + terminalIPAddress);
        logger.debug("maxHops = " + maxHops);
        logger.debug("timeoutMsec = " + timeoutMsec);
        logger.debug("callbackUrl = " + callbackUrl);
        logger.debug("clientMessage = " + clientMessage);
        logger.debug("userAgent = " + userAgent);
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
}
