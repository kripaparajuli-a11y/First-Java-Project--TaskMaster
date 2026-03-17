package controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private Button loginButton;

    @FXML
    public void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter email and password!");
            messageLabel.setStyle("-fx-text-fill: red;");
        } else if (email.equals("student@taskmaster.com") 
                && password.equals("1234")) {
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/dashboard.fxml"));
                javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 800, 600);
                javafx.stage.Stage stage = (javafx.stage.Stage) loginButton.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("TaskMaster - Dashboard");
            } catch (Exception e) {
                messageLabel.setText("Error loading dashboard!");
                System.out.println(e.getMessage());
            }
        } else {
            messageLabel.setText("Invalid email or password!");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }
}