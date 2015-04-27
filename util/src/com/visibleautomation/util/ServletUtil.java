package com.visibleautomation.util;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.BufferedReader;
import java.io.IOException;

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
}
