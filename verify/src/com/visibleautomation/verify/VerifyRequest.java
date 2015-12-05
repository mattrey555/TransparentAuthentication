package com.visibleautomation.verify;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

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

    // timeout to connect to clinet
    private static final String TIMEOUT_MSEC = "timeoutMsec"; 

    // callback URL for posting traceroute
    private static final String CALLBACK_URL = "callbackUrl"; 

    private String phoneNumber;
    private String clientId;
    private String terminalIPAddress;
    private String siteIPAddress;
    private String callbackUrl;
    private String requestId;
    private int maxHops = 16;
    private int timeoutMsec = 20000;

    public VerifyRequest(String queryString) throws Exception {
		String[] pairs = queryString.split("&");
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
			if (param.equals(CALLBACK_URL)) {
				callbackUrl = value;
			}               
		}           
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

	public void log() {
        System.out.println("parameters parsed");
        System.out.println("phoneNumber = " + phoneNumber);
        System.out.println("clientId = " + clientId);
        System.out.println("siteIPAddress = " + siteIPAddress);
        System.out.println("terminalIPAddress = " + terminalIPAddress);
        System.out.println("maxHops = " + maxHops);
        System.out.println("timeoutMsec = " + timeoutMsec);
        System.out.println("callbackUrl = " + callbackUrl);
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