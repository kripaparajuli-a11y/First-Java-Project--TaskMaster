package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import database.DatabaseManager;
import java.sql.*;
import java.security.MessageDigest;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginLinkButton;

    @FXML
    public void handleRegister() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        // Check empty fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields!", "red");
            return;
        }

        // Email validation
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showMessage("Please enter a valid email address!", "red");
            return;
        }

        // Password length check
        if (password.length() < 6) {
            showMessage("Password must be at least 6 characters!", "red");
            return;
        }

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            // Check if email already exists
            String checkSql = "SELECT * FROM student WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, email);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    showMessage("An account with this email already exists!", "red");
                    return;
                }
            }

            // Hash password before storing
            String hashedPassword = hashPassword(password);
            if (hashedPassword == null) {
                showMessage("Error processing password. Please try again.", "red");
                return;
            }

            // Insert new student with hashed password
            String sql = "INSERT INTO student (name, email, password) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, email);
                stmt.setString(3, hashedPassword);
                stmt.executeUpdate();
            }

            showMessage("Account created successfully! You can now log in.", "green");
            clearFields();

        } catch (SQLException e) {
            showMessage("Error creating account: " + e.getMessage(), "red");
        }
    }

    // SHA-256 password hashing
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            System.out.println("Hashing failed: " + e.getMessage());
            return null;
        }
    }

    @FXML
    public void goToLogin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 1000, 620);
            javafx.stage.Stage stage =
                (javafx.stage.Stage) loginLinkButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("TaskMaster");
        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), "red");
        }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    private void clearFields() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
    }
}