package com.visibleautomation.xmpp;
import java.util.List;
import java.util.ArrayList;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * This variant of XMPPTCPCOnnection maintains a list of custom packet listeners with associated callbacks
 * If the listener is called when an upstream XMPP message is received, its callback is called, and it 
 * is responsible for removing itself from the list.  If the callback hasn't been called before the timeout
 * expires, then it calls the expired() function and removes it from the list.
 * The list of callback maintains a natural sorted order, where new callbacks are added to the end of the list
 * and expired callbacks are removed from the front
 */
public class XMPPTCPStanzaConnection extends XMPPTCPConnection {
    public List<ListenerInfo> listenerList;
	private static Logger logger;

    public XMPPTCPStanzaConnection(XMPPTCPConnectionConfiguration config) {
    	super(config);
		logger = LogManager.getLogger(XMPPTCPStanzaConnection.class);
		logger.debug("initialization");
		listenerList = new ArrayList<ListenerInfo>();
    }

	/**
	 * Add a packet listerner which filters on a tag-value pair with a timeout.  This adds the listener
	 * to the end of the list, so it can be removed, and the callback.expired() function called if the XMPP
	 * uprstream messa
	 */
    public void addAsyncStanzaListener(SmackCcsClient.GcmStanzaListener packetListener, StanzaFilter packetFilter) {
    	super.addAsyncStanzaListener(packetListener, packetFilter);
		listenerList.add(new ListenerInfo(packetListener, packetFilter));
    }

	/**
	 * remove the listener associated with this callback
	 * @param callback callback to search for.
	 */
	public boolean removeAssociatedStanzaListener(SmackCcsClient.GcmStanzaCallback callback) {
		ListenerInfo associatedListener = null;
		for (ListenerInfo listener : listenerList) {
			if (listener.getListener().getCallback().equals(callback)) {
				associatedListener = listener;
			}
		}
		if (associatedListener != null) {
			listenerList.remove(associatedListener);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * scan from the front of the list for any callbacks whose timeout has expired.  Call their expired function
	 * and remove them from the list.
	 * @param currentTimeMillis() : System.currentTimeMillis()
	 */
	private long lastCurrentTimeMillis = 0;
	public void callAndRemoveExpired(long currentTimeMillis) {
		while (!listenerList.isEmpty()) {
			ListenerInfo listenerInfo = listenerList.get(0);
			if (currentTimeMillis - lastCurrentTimeMillis > 1000) {
				logger.debug("installedTimeSec = " + listenerInfo.getInstallTimeMsec()/1000 + 
								   " timeout sec =  " + listenerInfo.getListener().getTimeoutMsec()/1000 + 
								   " current time sec = " + currentTimeMillis/1000);
				lastCurrentTimeMillis = currentTimeMillis;
			}
			if (listenerInfo.getInstallTimeMsec() + listenerInfo.getListener().getTimeoutMsec() < currentTimeMillis) {
				logger.debug("callback expired");
				listenerInfo.getListener().getCallback().expired();
				listenerList.remove(0);
			} else {
				break;
			}
		}
	}

	/**
	 * Private class which hoids the listener, filter, and timeout.
	 */
	private class ListenerInfo {
		private SmackCcsClient.GcmStanzaListener packetListener;
		private StanzaFilter packetFilter;
		private long installTimeMsec;

		public ListenerInfo(SmackCcsClient.GcmStanzaListener packetListener, StanzaFilter packetFilter) {
			installTimeMsec = System.currentTimeMillis();
			this.packetListener = packetListener;
			this.packetFilter = packetFilter;
		}

		public long getInstallTimeMsec() {
			return installTimeMsec;
		}

		public SmackCcsClient.GcmStanzaListener getListener() {
			return packetListener;
		}
	}
}
