package org.example.login;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Servlet implementation class Register
 */
public class ProcessRegisterServlet extends HttpServlet {
	private static final String SQL_USER = "third_party";
	private static final String SQL_PASSWORD = "FiatX1/9";
	private static final String SELECT_USERNAME_PASSWORD = "SELECT * FROM USER WHERE USER_ID=? AND PWD=?";
	// ID autoincrement
	private static final String INSERT_USER = "INSERT INTO USER VALUES(0, ?, ?, ?, ?)";

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {

		try {	    
			String username  = request.getParameter("username");
			String password  = request.getParameter("password");
			String phone  = request.getParameter("phone");

			// prefix with a "1" so it matches the actual phone #
			if (phone.charAt(0) != '1') {
				phone = "1" + phone;
			}
			System.out.println("username = " + username + " password = " + password);
			String userId = verifyLogin(username, password);
			if (userId != null) {
				response.sendRedirect("register_failed.jsp");
			} else {
				addUser(username, password, phone);
				response.sendRedirect("register_successful.jsp");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void addUser(String username, String password, String phone) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Connection con = DriverManager.getConnection(Constants.DB_CONNECTION, SQL_USER, SQL_PASSWORD);
		try {
            PreparedStatement insertStatement = con.prepareStatement(INSERT_USER);
			String clientUserId = UUID.randomUUID().toString();
            insertStatement.setString(1, clientUserId);
            insertStatement.setString(2, username);
            insertStatement.setString(3, password);
            insertStatement.setString(4, phone);
			insertStatement.execute();
        } finally {
            con.close();
        }
	}
			

    private String verifyLogin(String username, String password) throws Exception {
        String userId = null;
        Class.forName("com.mysql.jdbc.Driver");
        Connection con = DriverManager.getConnection(Constants.DB_CONNECTION, SQL_USER, SQL_PASSWORD);
        try {
            PreparedStatement selectStatement = con.prepareStatement(SELECT_USERNAME_PASSWORD);
            selectStatement.setString(1, username);
            selectStatement.setString(2, password);
			System.out.println("select statement = " + selectStatement.toString());
            ResultSet rs = selectStatement.executeQuery();
            if (rs.first()) {
                int clientUserIdColIndex = rs.findColumn("CLIENT_USER_ID");
                userId = rs.getString(clientUserIdColIndex);
                System.out.println("and the user id is " + userId);
                rs.close();
				return userId;
            } 
        } finally {
            con.close();
        }
        return null;
    }
}
