package com.visibleautomation.verify;
import java.util.HashMap;

public class RequestSet {
	private HashMap<String, ResponseData> mRequestMap = null;
	private static RequestSet mInstance = null;

	public RequestSet() {
		mRequestMap = new HashMap<String, ResponseData>();
	} 

	public static RequestSet getInstance() {
		if (mInstance == null) {
			mInstance = new RequestSet();
		}
		return mInstance;
	}

	public void addRequest(String request, ResponseData response) {
		mRequestMap.put(request, response);
	}

	public void removeRequest(String request) {
		mRequestMap.remove(request);
	}

	public ResponseData getResponseData(String request) {
		return mRequestMap.get(request);
	}

	public ResponseData waitOnResponse(String request, long timeoutMsec) {
		ResponseData response = mRequestMap.get(request);
		System.out.println("waitOnResponse " + request);
		synchronized (response) {
			try {
				response.wait(timeoutMsec);
			} catch (InterruptedException iex) {
			}
		}
		removeRequest(request);
		return response;
	}

	public void notify(String request) {
		ResponseData response = mRequestMap.get(request);
		System.out.println("notify " + request);
		synchronized (response) {
			response.notify();
		}
	}
}
	
