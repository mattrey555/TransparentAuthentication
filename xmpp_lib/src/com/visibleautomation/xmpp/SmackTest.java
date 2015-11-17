package com.visibleautomation.xmpp;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.DefaultExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.xmlpull.v1.XmlPullParser;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.roster.Roster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


import javax.net.ssl.SSLSocketFactory;

/**
 * Sample Smack implementation of a client for GCM Cloud Connection Server. This
 * code can be run as a standalone CCS client.
 *
 * <p>For illustration purposes only.
 */
public class SmackTest {
     private static final String YOUR_API_KEY = "AIzaSyDUuok4W2TqMR9vKmkQd66Fm9j3SYxhHQo";
     private static final String YOUR_PROJECT_ID = "1012198772634";
     private static final String YOUR_PHONE_REG_ID = "APA91bFD5cQ-6Byhk5ZyXCnICB6D4zbmzFBifhiIHZTxYqqEGtuzX2q8eNJFxzjBZpwQRf-maLGRK9L5yvv3_y07lo0Bue6-nEElYeHhq9aelzSvS5xdV6_5nkrNFKfSFZ-MIrJIp97y-6AEG5VSexfa2TUtUcOiRz4Hns-6Vky9NgAFc5x4oQs";
    

    public static void main(String[] args) throws Exception {
        
        SmackCcsClient ccsClient = new SmackCcsClient();

        ccsClient.connect(YOUR_PROJECT_ID, YOUR_API_KEY);
        
        // Send a sample hello downstream message to a device.
        String messageId = ccsClient.nextMessageId();
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("Message", "Ahha, it works!");
        payload.put("CCS", "Dummy Message");
        payload.put("EmbeddedMessageId", messageId);
        String collapseKey = "sample";
        Long timeToLive = 10000L;
        String message = SmackCcsClient.createJsonMessage(YOUR_PHONE_REG_ID, messageId, payload, collapseKey, timeToLive, true);

        ccsClient.sendDownstreamMessage(message);
        System.out.println("Message sent.");
        
        //crude loop to keep connection open for receiving messages
        while(true)
        {;}
    }
}
