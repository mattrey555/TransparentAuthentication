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
public class ProcessTokenServlet extends HttpServlet {
	private static final String SELECT_SESSION_ID = "SELECT TOKEN from SESSION WHERE SESSION_ID=?";
	private static final String DELETE_SESSION_ID = "DELETE from SESSION WHERE SESSION_ID=?";
	private static final String URL_PARAM_SESSION_ID = "sessionId";
	private static final String URL_PARAM_TOKEN = "token";
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

    public ProcessTokenServlet() {
		System.out.println("ProcessTokenServlet: constructor  called");
    }

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
		try {
		 	String sessionId  = request.getParameter(URL_PARAM_SESSION_ID);
		 	String token  = request.getParameter(URL_PARAM_TOKEN);
			System.out.println("ProcessTokenServlet: sessionId = " + sessionId + " token = " + token);
			if (matchToken(sessionId, token)) {
				response.sendRedirect("verify_successful.jsp");
			} else {
				response.sendRedirect("verify_failed.jsp");
			}
			deleteSession(sessionId);
		} catch (SQLException sqlex) {
			System.out.println("failed to select session UUID " + sqlex.getMessage());
		}
	}

	public void deleteSession(String sessionId) throws SQLException {
		PreparedStatement deleteStatement = sDBConnection.prepareStatement(DELETE_SESSION_ID);
		deleteStatement.setString(1, sessionId);
		deleteStatement.execute();
	}
		

	public boolean matchToken(String sessionId, String token) throws SQLException {
		PreparedStatement selectStatement = sDBConnection.prepareStatement(SELECT_SESSION_ID);
		selectStatement.setString(1, sessionId);
	    ResultSet rs = selectStatement.executeQuery();
		int tokenIndex = rs.findColumn("TOKEN");
		if (rs.first()) {
			String dbToken = rs.getString(tokenIndex);
			return token.equals(dbToken);
		}
		return false;
	}
}
