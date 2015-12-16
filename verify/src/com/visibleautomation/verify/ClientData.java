package com.visibleautomation.verify;
import java.lang.Integer;
import java.net.URLDecoder;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Date;
import java.lang.Thread;
import java.lang.Runnable;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

// subclass containing the phone number, messaging ID, and public key for a specific client.
public class ClientData {
    private static final String SELECT_BY_PHONE = "select * from user where phone_number=?";
    private String phoneNumber;
    private String gcmMessagingId;
    private String publicKey;
	private static Logger logger = LogManager.getLogger(ClientData.class);

    public ClientData(Connection con, String phoneNumber) throws Exception {
		this.phoneNumber = phoneNumber;
		String userId = null;
	    PreparedStatement selectStatement = con.prepareStatement(SELECT_BY_PHONE);
	    selectStatement.setString(1, phoneNumber);
	    ResultSet rs = selectStatement.executeQuery();
	    if (rs.first()) {
			logger.debug("there is a matching record for " + phoneNumber);
			int phoneNumberColIndex = rs.findColumn("PHONE_NUMBER");
			String testPhoneNumber = rs.getString(phoneNumberColIndex);
			int clientUserIdColIndex = rs.findColumn("USER_ID");
			gcmMessagingId = rs.getString(clientUserIdColIndex);
			int publicKeyColIndex = rs.findColumn("PUBLIC_KEY");
			publicKey = rs.getString(publicKeyColIndex);
			logger.debug("and the user id is " + userId);
			rs.close();
	    } else {
			logger.error("there was no user matching " + phoneNumber);
	    }
    }

    public String getPhoneNumber() {
		return phoneNumber;
    }

    public String getPublicKey() {
		return publicKey;
    }

    public String getGCMMessagingId() {
		return gcmMessagingId;
    }
}
