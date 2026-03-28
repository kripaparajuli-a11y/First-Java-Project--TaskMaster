package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import controller.GpaController;
import javafx.fxml.FXML;

public class DatabaseManager {
    // Singleton instance
    private static DatabaseManager instance;
    private Connection connection;

    // Database credentials
    private static final String URL = "jdbc:mysql://localhost:3306/taskmaster";
    private static final String USER = "root";
    private static final String PASSWORD = "qwe123@QWE";

    // Private constructor - prevents direct instantiation
    private DatabaseManager() {
        connect();
    }

    // Singleton - only one instance exists
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
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

    // Get connection
    public Connection getConnection() {
        return connection;
    }

    // Execute query (SELECT)
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

    // Execute update (INSERT, UPDATE, DELETE)
    public int executeUpdate(String sql, Object... params) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
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