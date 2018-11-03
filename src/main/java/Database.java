import java.sql.*;

public class Database {
	
	private Connection connection = null;
	private boolean isConnectionValid = false;
	
	public Database() {
		try {
			DriverManager.setLoginTimeout(5);
			
			String host = DB.HOSTNAME;
			String database = DB.DATABASE;
			String username = DB.USERNAME;
			String password = DB.PASSWORD;
			
			connection = DriverManager.getConnection("jdbc:mariadb://" + host + ":3306/" + database, username, password);
			isConnectionValid = true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isValid() {
		return this.isConnectionValid;
	}
	
	
	public ResultSet execute(String query) {
		try {
			Statement statement = connection.createStatement();
			return statement.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public ResultSet execute(String query, Object... params) {
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			for (int i = 0; i < params.length; i++) {
				statement.setObject(i + 1, params[i]);
			}
			return statement.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void update(String query) {
		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void update(String query, Object... params) {
		try {
			PreparedStatement statement = connection.prepareStatement(query);
			for (int i = 0; i < params.length; i++) {
				statement.setObject(i+1, params[i]);
			}
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}