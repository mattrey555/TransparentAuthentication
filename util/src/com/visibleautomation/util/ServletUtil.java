package com.visibleautomation.util;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URL;
import java.net.URLConnection;
import java.io.UnsupportedEncodingException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;

public class ServletUtil {

    public static String readPostData(HttpServletRequest request) throws IOException {
        BufferedReader br = request.getReader();
        StringBuffer sb = new StringBuffer();
        String line = null;
        do {
            line = br.readLine();
            if (line != null) {
                sb.append(line);
            }
        } while (line != null);
        return sb.toString();
    }

    public static String getParam(String pair) throws UnsupportedEncodingException {
        int idx = pair.indexOf("=");
        return URLDecoder.decode(pair.substring(0, idx), "UTF-8");
    }
    // get the value for the spectified parameter name
    public static String getValue(String pair) throws UnsupportedEncodingException {
        int idx = pair.indexOf("=");
        return URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
    }

	/**
	 * post string data to a specified URL
	 * @param url url to post data to 
	 * @param postData post data.
	 */
	public static HttpURLConnection postUrlString(URL url, String postData) throws IOException {
		HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		urlConnection.setRequestMethod("POST");
		urlConnection.setDoOutput(true);
		System.out.println("writing postData " + postData);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
		bw.write(postData);
		bw.flush();
		urlConnection.getOutputStream().close();
		return urlConnection;
	}
} 

