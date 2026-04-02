package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Task;
import database.DatabaseManager;
import java.sql.*;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> courseColumn;
    @FXML private TableColumn<Task, String> deadlineColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TextField titleField;
    @FXML private TextField courseField;
    @FXML private TextField deadlineField;
    @FXML private ComboBox<String> priorityBox;
    @FXML private ComboBox<String> typeBox;
    @FXML private Label messageLabel;

    private ObservableList<Task> taskList = FXCollections.observableArrayList();
    private int currentStudentId = 1;

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, Kripa!");

        titleColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getTitle()));
        courseColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getCourseId()));
        deadlineColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getDeadline()));
        priorityColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getPriority()));
        statusColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));

        priorityBox.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        typeBox.setItems(FXCollections.observableArrayList(
            "Assignment", "Exam", "Project", "Quiz", "Presentation"));

        taskTable.setItems(taskList);
        loadTasksFromDatabase();
    }

    public void setStudentInfo(String name, int studentId) {
        welcomeLabel.setText("Welcome, " + name + "!");
        this.currentStudentId = studentId;
        loadTasksFromDatabase();
        checkUpcomingDeadlines();
    }

    private void loadTasksFromDatabase() {
        taskList.clear();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            // Only load tasks belonging to the logged-in student
            String sql = "SELECT * FROM task WHERE student_id = " + currentStudentId;
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Task task = new Task(
                    String.valueOf(rs.getInt("task_id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("deadline"),
                    rs.getString("priority"),
                    rs.getString("status"),
                    rs.getString("course_id"),
                    "1",
                    rs.getString("task_type")
                );
                taskList.add(task);
            }
        } catch (SQLException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
    }

    @FXML
    public void handleAddTask() {
        String title = titleField.getText();
        String course = courseField.getText();
        String deadline = deadlineField.getText();
        String priority = priorityBox.getValue();
        String type = typeBox.getValue();

       if (title.isEmpty() || course.isEmpty() ||
            deadline.isEmpty() || priority == null || type == null) {
            messageLabel.setText("Please fill in all fields!");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }


        try {
            java.time.LocalDate deadlineDate = java.time.LocalDate.parse(deadline);
            java.time.LocalDate today = java.time.LocalDate.now();
            if (deadlineDate.isBefore(today)) {
                messageLabel.setText("Deadline cannot be in the past!");
                messageLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        } catch (Exception e) {
            messageLabel.setText("Invalid date format! Use YYYY-MM-DD (e.g. 2026-04-11)");
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        } 

        try {
            // First insert course if not exists
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement courseStmt = conn.prepareStatement(
                "INSERT INTO course (course_name) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS);
            courseStmt.setString(1, course);
            courseStmt.executeUpdate();
            ResultSet courseRs = courseStmt.getGeneratedKeys();
            int courseId = 1;
            if (courseRs.next()) courseId = courseRs.getInt(1);

            // Insert task
            String sql = "INSERT INTO task (student_id, course_id, title, description, " +
            "deadline, priority, status, task_type) VALUES (?, ?, ?, '', ?, ?, 'Not Started', ?)";
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            // When adding a task, save it under the correct student
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, courseId);
            stmt.setString(3, title);
            stmt.setString(4, deadline);
            stmt.setString(5, priority);
            stmt.setString(6, type);
            stmt.executeUpdate(); 

            messageLabel.setText("Task saved!");
            messageLabel.setStyle("-fx-text-fill: green;");
            clearFields();
            loadTasksFromDatabase();

        } catch (SQLException e) {
            messageLabel.setText("Error saving task!");
            System.out.println("Error: " + e.getMessage());
        }
    }

    @FXML
    public void handleDeleteTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                String sql = "DELETE FROM task WHERE task_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, Integer.parseInt(selected.getTaskId()));
                stmt.executeUpdate();
                messageLabel.setText("Task deleted!");
                messageLabel.setStyle("-fx-text-fill: green;");
                loadTasksFromDatabase();
            } catch (SQLException e) {
                System.out.println("Error deleting: " + e.getMessage());
            }
        } else {
            messageLabel.setText("Please select a task to delete!");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleMarkComplete() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                String sql = "UPDATE task SET status = 'Completed' WHERE task_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setInt(1, Integer.parseInt(selected.getTaskId()));
                stmt.executeUpdate();
                messageLabel.setText("Task marked as completed!");
                messageLabel.setStyle("-fx-text-fill: green;");
                loadTasksFromDatabase();
            } catch (SQLException e) {
                System.out.println("Error updating: " + e.getMessage());
            }
        } else {
            messageLabel.setText("Please select a task first!");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void clearFields() {
        titleField.clear();
        courseField.clear();
        deadlineField.clear();
        priorityBox.setValue(null);
        typeBox.setValue(null);
    }
    @FXML
    public void openGpa() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/gpa.fxml"));
            javafx.scene.Parent root = loader.load();
            GpaController gpa = loader.getController();
            gpa.setStudentId(currentStudentId);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 650);
            javafx.stage.Stage stage =
                (javafx.stage.Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("TaskMaster - GPA Calculator");
        } catch (Exception e) {
            System.out.println("Error opening GPA: " + e.getMessage());
        }
    }
    @FXML
    public void openStudyTimer() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/study_session.fxml"));
            javafx.scene.Parent root = loader.load();
            StudySessionController studySession = loader.getController();
            studySession.setStudentId(currentStudentId);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 650);
            javafx.stage.Stage stage =
                (javafx.stage.Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("TaskMaster - Study Timer");
        } catch (Exception e) {
            System.out.println("Error opening Study Timer: " + e.getMessage());
        }
    }
    private void checkUpcomingDeadlines() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT title, deadline FROM task WHERE student_id = ? " +
                        "AND status != 'Completed' AND deadline BETWEEN CURDATE() " +
                        "AND DATE_ADD(CURDATE(), INTERVAL 3 DAY)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            StringBuilder alertMessage = new StringBuilder();
            final int[] count = {0};

            while (rs.next()) {
                alertMessage.append("• ")
                        .append(rs.getString("title"))
                        .append(" — due ")
                        .append(rs.getString("deadline"))
                        .append("\n");
                count[0]++;
            }

            if (count[0] > 0) {
                javafx.application.Platform.runLater(() -> {
                    javafx.scene.control.Alert alert =
                        new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.WARNING);
                    alert.setTitle("Upcoming Deadlines!");
                    alert.setHeaderText("You have " + count[0] + " task(s) due in the next 3 days:");
                    alert.setContentText(alertMessage.toString());
                    alert.showAndWait();
                });
            }

        } catch (SQLException e) {
            System.out.println("Error checking deadlines: " + e.getMessage());
        }
    }
    @FXML
    public void openCharts() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/chart.fxml"));
            javafx.scene.Parent root = loader.load();
            ChartController chart = loader.getController();
            chart.setStudentId(currentStudentId);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 650);
            javafx.stage.Stage stage =
                (javafx.stage.Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("TaskMaster - Workload Charts");
        } catch (Exception e) {
            System.out.println("Error opening Charts: " + e.getMessage());
        }
    }
}