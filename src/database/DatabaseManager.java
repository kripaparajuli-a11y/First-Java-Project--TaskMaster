package database;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    // Singleton - eager initialization (thread-safe, no null check needed)
    private static final DatabaseManager instance = new DatabaseManager();
    private Connection connection;

    // Loaded from db.properties, NOT hardcoded
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    // Private constructor
    private DatabaseManager() {
        loadConfig();
        connect();
    }

    // Load DB credentials from config file
    private void loadConfig() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                System.out.println("db.properties not found!");
                return;
            }
            Properties prop = new Properties();
            prop.load(input);
            URL      = prop.getProperty("db.url");
            USER     = prop.getProperty("db.user");
            PASSWORD = prop.getProperty("db.password");
        } catch (Exception e) {
            System.out.println("Failed to load config: " + e.getMessage());
        }
    }

    // Singleton accessor
    public static DatabaseManager getInstance() {
        return instance;
    }

    // Connect to database
    private void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    // Get connection - reconnects if dropped
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("Reconnecting to database...");
                connect();
            }
        } catch (SQLException e) {
            System.out.println("Connection check failed, reconnecting: " + e.getMessage());
            connect();
        }
        return connection;
    }

    // Execute SELECT query
    public ResultSet executeQuery(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeQuery();
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
            return null;
        }
    }

    // Execute INSERT/UPDATE/DELETE - uses try-with-resources to close statement
    public int executeUpdate(String sql, Object... params) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Update failed: " + e.getMessage());
            return 0;
        }
    }

    // Close connection
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
}