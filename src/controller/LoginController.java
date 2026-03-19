package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import database.DatabaseManager;
import java.sql.*;

public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;
    @FXML private Button registerButton;

    @FXML
    public void handleLogin() throws Exception {
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showMessage("Please enter your email and password!", "red");
            return;
        }

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT * FROM student WHERE email = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Login successful — get student name
                String studentName = rs.getString("name");
                int studentId = rs.getInt("student_id");

                showMessage("Login Successful!", "green");

                // Load dashboard
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/dashboard.fxml"));
                javafx.scene.Parent root = loader.load();

                // Pass student info to dashboard
                DashboardController dashboard = loader.getController();
                dashboard.setStudentInfo(studentName, studentId);

                javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 650);
                javafx.stage.Stage stage = 
                    (javafx.stage.Stage) loginButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("TaskMaster - Dashboard");

            } else {
                showMessage("Invalid email or password!", "red");
            }

        } catch (SQLException e) {
            showMessage("Database error: " + e.getMessage(), "red");
        }
    }

    @FXML
    public void goToRegister() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/register.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 500, 580);
            javafx.stage.Stage stage = 
                (javafx.stage.Stage) registerButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("TaskMaster - Register");
        } catch (Exception e) {
            showMessage("Error: " + e.getMessage(), "red");
        }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}