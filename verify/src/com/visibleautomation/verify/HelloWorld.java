package com.visibleautomation.verify;
import java.io.IOException;
import java.io.*;
import javax.servlet.*;
import java.net.URL;
import java.util.Map;
// 3.0
//import javax.servlet.annotation.*;
import javax.servlet.http.*;

// 3.0 
//@WebServlet(name = "Hello World Servlet", urlPatterns = { "/hello" }) 
public class HelloWorld extends HttpServlet {
 
  private String message;

  public HelloWorld() {
	System.out.println("constructor is called");
  }

  public void init() throws ServletException
  {
      // Do required initialization
      message = "Hello World";
      System.out.println("init is getting called");
  }

  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
            throws ServletException, IOException
  {
      // Set response content type
      String urlStr = request.getRequestURL().toString();
      URL url = new URL(urlStr); 
      response.setContentType("text/html");
      System.out.println("doGet is getting called");
	String ipAddress = request.getHeader("X-FORWARDED-FOR");
	if (ipAddress == null) {
    	    ipAddress = request.getRemoteAddr();
	}

      // Actual logic goes here.
      PrintWriter out = response.getWriter();
      out.println("<h1>" + "query string: " + request.getQueryString() + "</h1>");
      out.println("<h1>" + "URL: " + url.toString() + "</h1>");
      out.println("<h1>" + "path: " + url.getPath() + "</h1>");
      out.println("<h1>" + "query: " + url.getQuery() + "</h1>");
      out.println("<h1>" + "userInfo: " + url.getUserInfo() + "</h1>");
      out.println("<h1>" + "request: " + request.getRequestURL().toString() + "</h1>");
      out.println("<h1>" + "IPAddress: " + ipAddress + "</h1>");

      Map<String, String[]> queryParams = request.getParameterMap();
      for (Map.Entry<String, String[]> iter : queryParams.entrySet()) {
	 out.println("<h1>" + "param: " + iter.getKey() + " = ");
	 for (String value : iter.getValue()) {
	    out.println(value + " "); 
	 }
      }
      out.println("</h1>");
  }
  
  public void destroy()
  {
      // do nothing.
  }
} 
