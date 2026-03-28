package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import database.DatabaseManager;
import java.sql.*;

public class GpaController {

    @FXML private TextField courseNameField;
    @FXML private TextField percentageField;
    @FXML private Label messageLabel;
    @FXML private Label gpaLabel;
    @FXML private TableView<String[]> gpaTable;
    @FXML private TableColumn<String[], String> courseColumn;
    @FXML private TableColumn<String[], String> percentageColumn;
    @FXML private TableColumn<String[], String> gpaColumn;
    @FXML private Button backButton;

    private ObservableList<String[]> gpaList = FXCollections.observableArrayList();
    private int currentStudentId = 1;

    public void setStudentId(int studentId) {
        this.currentStudentId = studentId;
        loadGpaFromDatabase();
    }

    @FXML
    public void initialize() {
        courseColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        percentageColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        gpaColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));

        gpaTable.setItems(gpaList);
    }

    private void loadGpaFromDatabase() {
        gpaList.clear();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT course_name, percentage, gpa_points FROM gpa WHERE student_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            double totalGpa = 0;
            int count = 0;

            while (rs.next()) {
                String[] row = {
                    rs.getString("course_name"),
                    rs.getDouble("percentage") + "%",
                    String.valueOf(rs.getDouble("gpa_points"))
                };
                gpaList.add(row);
                totalGpa += rs.getDouble("gpa_points");
                count++;
            }

            if (count > 0) {
                double average = Math.round((totalGpa / count) * 100.0) / 100.0;
                gpaLabel.setText("Your GPA: " + average + " / 4.0");
                gpaLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 16px; -fx-font-weight: bold;");
            } else {
                gpaLabel.setText("No grades added yet.");
                gpaLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            }

        } catch (SQLException e) {
            System.out.println("Error loading GPA: " + e.getMessage());
        }
    }

    private double calculateGpaPoints(double percentage) {
        if (percentage >= 90) return 4.0;
        else if (percentage >= 80) return 3.7;
        else if (percentage >= 70) return 3.0;
        else if (percentage >= 60) return 2.7;
        else if (percentage >= 50) return 2.0;
        else return 0.0;
    }

    @FXML
    public void handleAddGrade() {
        String courseName = courseNameField.getText().trim();
        String percentageText = percentageField.getText().trim();

        if (courseName.isEmpty() || percentageText.isEmpty()) {
            showMessage("Please fill in all fields!", "red");
            return;
        }

        double percentage;
        try {
            percentage = Double.parseDouble(percentageText);
            if (percentage < 0 || percentage > 100) {
                showMessage("Percentage must be between 0 and 100!", "red");
                return;
            }
        } catch (NumberFormatException e) {
            showMessage("Please enter a valid number for percentage!", "red");
            return;
        }

        double gpaPoints = calculateGpaPoints(percentage);

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            // Check if course already exists for this student
            String checkSql = "SELECT gpa_id FROM gpa WHERE student_id = ? AND course_name = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, currentStudentId);
            checkStmt.setString(2, courseName);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Update existing
                String updateSql = "UPDATE gpa SET percentage = ?, gpa_points = ? WHERE student_id = ? AND course_name = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setDouble(1, percentage);
                updateStmt.setDouble(2, gpaPoints);
                updateStmt.setInt(3, currentStudentId);
                updateStmt.setString(4, courseName);
                updateStmt.executeUpdate();
                showMessage("Grade updated successfully!", "green");
            } else {
                // Insert new
                String insertSql = "INSERT INTO gpa (student_id, course_name, percentage, gpa_points) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, currentStudentId);
                insertStmt.setString(2, courseName);
                insertStmt.setDouble(3, percentage);
                insertStmt.setDouble(4, gpaPoints);
                insertStmt.executeUpdate();
                showMessage("Grade added successfully!", "green");
            }

            courseNameField.clear();
            percentageField.clear();
            loadGpaFromDatabase();

        } catch (SQLException e) {
            showMessage("Error saving grade: " + e.getMessage(), "red");
        }
    }

    @FXML
    public void handleDeleteGrade() {
        String[] selected = gpaTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                String sql = "DELETE FROM gpa WHERE student_id = ? AND course_name = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, currentStudentId);
                stmt.setString(2, selected[0]);
                stmt.executeUpdate();
                showMessage("Grade deleted!", "green");
                loadGpaFromDatabase();
            } catch (SQLException e) {
                showMessage("Error deleting grade: " + e.getMessage(), "red");
            }
        } else {
            showMessage("Please select a grade to delete!", "red");
        }
    }

    @FXML
    public void goBack() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            DashboardController dashboard = loader.getController();
            dashboard.setStudentInfo("Student", currentStudentId);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 650);
            javafx.stage.Stage stage =
                (javafx.stage.Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("TaskMaster - Dashboard");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}
