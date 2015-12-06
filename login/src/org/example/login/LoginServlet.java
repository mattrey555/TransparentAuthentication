package org.example.login;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.RequestDispatcher;
import org.json.JSONObject;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.Properties;
import java.util.UUID;

/**
 * Login servlet: initialize the session ID
 */
public class LoginServlet extends HttpServlet {
	private static final String INSERT_SESSION_ID = "INSERT INTO SESSION (SESSION_ID) VALUES (?)";
	private static Connection sDBConnection;
	
	static {
		System.out.println("LoginServlet: static initialization for sql.properties");
		try {
			Constants.setDatabaseVariables();
			Class.forName("com.mysql.jdbc.Driver");
			String dbConnection = String.format(Constants.DB_CONNECTION_FORMAT, Constants.getDBDatabase());
			sDBConnection = DriverManager.getConnection(dbConnection, Constants.getDBUsername(), Constants.getDBPassword());
		} catch (Exception ex) {
			System.out.println("failed to initialize database " + ex.getMessage());
		}

	}

    public LoginServlet() {
		System.out.println("LoginServlet: constructor  called");
    }

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
		try {
			UUID sessionID = UUID.randomUUID();
			PreparedStatement insertStatement = sDBConnection.prepareStatement(INSERT_SESSION_ID);
	    	insertStatement.setString(1, sessionID.toString());
			insertStatement.execute();
			RequestDispatcher requestDispatcher = request.getRequestDispatcher("/login.jsp");
			request.setAttribute("sessionId", sessionID);
			requestDispatcher.forward(request, response);
		} catch (SQLException sqlex) {
			System.out.println("failed to insert session UUID " + sqlex.getMessage());
		}
	}
}
