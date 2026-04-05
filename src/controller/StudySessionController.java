package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import database.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

public class StudySessionController {

    @FXML private ComboBox<String> taskComboBox;
    @FXML private Label timerLabel;
    @FXML private Label messageLabel;
    @FXML private Label sessionStatusLabel;
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button backButton;
    @FXML private TableView<String[]> sessionTable;
    @FXML private TableColumn<String[], String> taskColumn;
    @FXML private TableColumn<String[], String> startColumn;
    @FXML private TableColumn<String[], String> durationColumn;

    private int currentStudentId = 1;
    private String currentStudentName = "Student";
    private String currentStudentEmail = "";
    private int selectedTaskId = -1;
    private String selectedTaskTitle = "";
    private LocalDateTime startTime;
    private Timeline timer;
    private int secondsElapsed = 0;
    private boolean isRunning = false;

    private ObservableList<String[]> sessionList = FXCollections.observableArrayList();

    public void setStudentId(int studentId) {
        this.currentStudentId = studentId;
        loadTasks();
        loadSessions();
    }

    public void setStudentInfo(String name, String email, int studentId) {
        this.currentStudentName  = name;
        this.currentStudentEmail = email;
        this.currentStudentId    = studentId;
        loadTasks();
        loadSessions();
    }

    @FXML
    public void initialize() {
        taskColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue()[0]));
        startColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue()[1]));
        durationColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(data.getValue()[2]));

        sessionTable.setItems(sessionList);
        stopButton.setDisable(true);
        timerLabel.setText("00:00:00");
    }

    private void loadTasks() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT task_id, title FROM task WHERE student_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            ObservableList<String> tasks = FXCollections.observableArrayList();
            while (rs.next()) {
                tasks.add(rs.getInt("task_id") + " - " + rs.getString("title"));
            }
            taskComboBox.setItems(tasks);
        } catch (SQLException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
    }

    private void loadSessions() {
        sessionList.clear();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT task_title, start_time, duration_minutes " +
                        "FROM study_session WHERE student_id = ? ORDER BY start_time DESC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String[] row = {
                    rs.getString("task_title"),
                    rs.getString("start_time"),
                    rs.getInt("duration_minutes") + " mins"
                };
                sessionList.add(row);
            }
        } catch (SQLException e) {
            System.out.println("Error loading sessions: " + e.getMessage());
        }
    }

    @FXML
    public void handleStart() {
        String selected = taskComboBox.getValue();
        if (selected == null) {
            showMessage("Please select a task first!", "red");
            return;
        }
        String[] parts = selected.split(" - ", 2);
        selectedTaskId    = Integer.parseInt(parts[0]);
        selectedTaskTitle = parts[1];

        startTime      = LocalDateTime.now();
        secondsElapsed = 0;
        isRunning      = true;

        startButton.setDisable(true);
        stopButton.setDisable(false);
        taskComboBox.setDisable(true);

        sessionStatusLabel.setText("Studying: " + selectedTaskTitle);
        sessionStatusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            int h = secondsElapsed / 3600;
            int m = (secondsElapsed % 3600) / 60;
            int s = secondsElapsed % 60;
            timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        showMessage("Timer started! Study hard!", "green");
    }

    @FXML
    public void handleStop() {
        if (!isRunning) return;
        timer.stop();
        isRunning = false;

        LocalDateTime endTime   = LocalDateTime.now();
        int durationMinutes     = secondsElapsed / 60;
        if (durationMinutes < 1) durationMinutes = 1;

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "INSERT INTO study_session (student_id, task_id, task_title, " +
                        "start_time, end_time, duration_minutes) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, selectedTaskId);
            stmt.setString(3, selectedTaskTitle);
            stmt.setString(4, startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            stmt.setString(5, endTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            stmt.setInt(6, durationMinutes);
            stmt.executeUpdate();
            showMessage("Session saved! You studied for " + durationMinutes + " minute(s).", "green");
        } catch (SQLException e) {
            showMessage("Error saving session: " + e.getMessage(), "red");
        }

        startButton.setDisable(false);
        stopButton.setDisable(true);
        taskComboBox.setDisable(false);
        sessionStatusLabel.setText("Session stopped.");
        sessionStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
        timerLabel.setText("00:00:00");
        loadSessions();
    }

    @FXML
    public void goBack() {
        if (isRunning) timer.stop();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            DashboardController dashboard = loader.getController();
            dashboard.setStudentInfo(currentStudentName, currentStudentId, currentStudentEmail);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1100, 680);
            javafx.stage.Stage stage =
                (javafx.stage.Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("TaskMaster - My Tasks");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void showMessage(String msg, String color) {
        messageLabel.setText(msg);
        messageLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}