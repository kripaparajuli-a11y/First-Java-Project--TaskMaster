package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Task;
import database.DatabaseManager;
import java.sql.*;
import java.time.LocalDate;

public class DashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label studentNameLabel;
    @FXML private Label studentEmailLabel;

    @FXML private TableView<Task> taskTable;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> courseColumn;
    @FXML private TableColumn<Task, String> deadlineColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, String> notesColumn;
    @FXML private TableColumn<Task, String> typeColumn;

    @FXML private TextField titleField;
    @FXML private TextField courseField;
    @FXML private TextField deadlineField;
    @FXML private TextField notesField;
    @FXML private ComboBox<String> priorityBox;
    @FXML private ComboBox<String> typeBox;
    @FXML private ComboBox<String> filterBox;
    @FXML private Label messageLabel;

    private ObservableList<Task> taskList = FXCollections.observableArrayList();
    private int currentStudentId = 1;
    private String currentStudentName = "Student";
    private String currentStudentEmail = "";
    private boolean reminderShown = false;

    @FXML
    public void initialize() {
        if (titleColumn != null) {
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
            typeColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getTaskType()));
            notesColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));

            // ── Instant tooltip on Notes column ──
            notesColumn.setCellFactory(col -> new TableCell<Task, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setTooltip(null);
                    } else {
                        setText(item);
                        Tooltip tooltip = new Tooltip(item);
                        tooltip.setWrapText(true);
                        tooltip.setMaxWidth(300);
                        tooltip.setShowDelay(javafx.util.Duration.ZERO);
                        setTooltip(tooltip);
                    }
                }
            });

            priorityBox.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
            typeBox.setItems(FXCollections.observableArrayList(
                "Assignment", "Exam", "Project", "Quiz", "Presentation"));
            filterBox.setItems(FXCollections.observableArrayList(
                "All Tasks", "Not Started", "In Progress", "Completed", "Overdue"));
            taskTable.setItems(taskList);
        }
    }

    public void setStudentInfo(String name, int studentId, String email) {
        this.currentStudentName  = name;
        this.currentStudentId    = studentId;
        this.currentStudentEmail = email;
        updateSidebarProfile();
        autoMarkOverdue();
        if (!reminderShown) {
            reminderShown = true;
            checkUpcomingDeadlines();
        }
        loadTasksFromDatabase();
    }

    public void setStudentInfo(String name, int studentId) {
        setStudentInfo(name, studentId, this.currentStudentEmail);
    }

    private void updateSidebarProfile() {
        if (welcomeLabel != null)
            welcomeLabel.setText(currentStudentName + "!");
        if (studentNameLabel != null)
            studentNameLabel.setText(currentStudentName);
        if (studentEmailLabel != null)
            studentEmailLabel.setText(currentStudentEmail);
    }

    private void autoMarkOverdue() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "UPDATE task SET status = 'Overdue' WHERE student_id = ? " +
                         "AND deadline < CURDATE() AND status != 'Completed'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error marking overdue: " + e.getMessage());
        }
    }

    private void loadTasksFromDatabase() {
        taskList.clear();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT t.*, c.course_name FROM task t " +
                         "LEFT JOIN course c ON t.course_id = c.course_id " +
                         "WHERE t.student_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Task task = new Task(
                    String.valueOf(rs.getInt("task_id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("deadline"),
                    rs.getString("priority"),
                    rs.getString("status"),
                    rs.getString("course_name"),
                    String.valueOf(currentStudentId),
                    rs.getString("task_type")
                );
                taskList.add(task);
            }
        } catch (SQLException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
    }

    @FXML
    public void handleFilter() {
        String filter = filterBox.getValue();
        if (filter == null || filter.equals("All Tasks")) {
            taskTable.setItems(taskList);
            return;
        }
        ObservableList<Task> filtered = FXCollections.observableArrayList();
        for (Task t : taskList) {
            if (t.getStatus().equals(filter)) filtered.add(t);
        }
        taskTable.setItems(filtered);
    }

    @FXML
    public void handleAddTask() {
        String title    = titleField.getText().trim();
        String course   = courseField.getText().trim();
        String deadline = deadlineField.getText().trim();
        String priority = priorityBox.getValue();
        String type     = typeBox.getValue();
        String notes    = notesField.getText().trim();

        if (title.isEmpty() || course.isEmpty() ||
                deadline.isEmpty() || priority == null || type == null) {
            showMessage("Please fill in all required fields!", "red");
            return;
        }

        try { LocalDate.parse(deadline); }
        catch (Exception e) {
            showMessage("Invalid date! Use YYYY-MM-DD", "red");
            return;
        }

        String status = LocalDate.parse(deadline).isBefore(LocalDate.now())
            ? "Overdue" : "Not Started";

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            int courseId = 1;
            PreparedStatement findCourse = conn.prepareStatement(
                "SELECT course_id FROM course WHERE course_name = ?");
            findCourse.setString(1, course);
            ResultSet existingCourse = findCourse.executeQuery();
            if (existingCourse.next()) {
                courseId = existingCourse.getInt(1);
            } else {
                PreparedStatement insertCourse = conn.prepareStatement(
                    "INSERT INTO course (course_name) VALUES (?)",
                    Statement.RETURN_GENERATED_KEYS);
                insertCourse.setString(1, course);
                insertCourse.executeUpdate();
                ResultSet keys = insertCourse.getGeneratedKeys();
                if (keys.next()) courseId = keys.getInt(1);
            }

            String sql = "INSERT INTO task (student_id, course_id, title, description, " +
                         "deadline, priority, status, task_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, courseId);
            stmt.setString(3, title);
            stmt.setString(4, notes);
            stmt.setString(5, deadline);
            stmt.setString(6, priority);
            stmt.setString(7, status);
            stmt.setString(8, type);
            stmt.executeUpdate();

            showMessage("Task added successfully!", "green");
            clearFields();
            loadTasksFromDatabase();

        } catch (SQLException e) {
            showMessage("Error saving task: " + e.getMessage(), "red");
        }
    }

    @FXML
    public void handleEditTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showMessage("Please select a task to edit!", "red"); return; }
        titleField.setText(selected.getTitle());
        courseField.setText(selected.getCourseId());
        deadlineField.setText(selected.getDeadline());
        notesField.setText(selected.getDescription());
        priorityBox.setValue(selected.getPriority());
        typeBox.setValue(selected.getTaskType());
        titleField.setUserData(selected.getTaskId());
        showMessage("Edit the fields above then click Save Edit", "#a855f7");
    }

    @FXML
    public void handleSaveEdit() {
        if (titleField.getUserData() == null) {
            showMessage("Click Edit Task on a row first!", "red"); return;
        }
        String taskId   = (String) titleField.getUserData();
        String title    = titleField.getText().trim();
        String deadline = deadlineField.getText().trim();
        String priority = priorityBox.getValue();
        String type     = typeBox.getValue();
        String notes    = notesField.getText().trim();

        if (title.isEmpty() || deadline.isEmpty() || priority == null || type == null) {
            showMessage("Please fill in all fields!", "red"); return;
        }

        try { LocalDate.parse(deadline); }
        catch (Exception e) { showMessage("Invalid date! Use YYYY-MM-DD", "red"); return; }

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            // ── Recalculate status based on the new deadline ──
            String newStatus = LocalDate.parse(deadline).isBefore(LocalDate.now())
                ? "Overdue" : "Not Started";

            String sql = "UPDATE task SET title=?, description=?, deadline=?, " +
                         "priority=?, task_type=?, status=? WHERE task_id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, title);
            stmt.setString(2, notes);
            stmt.setString(3, deadline);
            stmt.setString(4, priority);
            stmt.setString(5, type);
            stmt.setString(6, newStatus);
            stmt.setInt(7, Integer.parseInt(taskId));
            stmt.executeUpdate();

            showMessage("Task updated successfully!", "green");
            clearFields();
            titleField.setUserData(null);
            loadTasksFromDatabase();
        } catch (SQLException e) {
            showMessage("Error updating: " + e.getMessage(), "red");
        }
    }

    @FXML
    public void handleMarkComplete() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showMessage("Select a task first!", "red"); return; }
        updateTaskStatus(selected.getTaskId(), "Completed");
        showMessage("Task marked as completed!", "green");
    }

    @FXML
    public void handleMarkInProgress() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showMessage("Select a task first!", "red"); return; }
        updateTaskStatus(selected.getTaskId(), "In Progress");
        showMessage("Task marked as in progress!", "#e67e22");
    }

    private void updateTaskStatus(String taskId, String status) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE task SET status = ? WHERE task_id = ?");
            stmt.setString(1, status);
            stmt.setInt(2, Integer.parseInt(taskId));
            stmt.executeUpdate();
            loadTasksFromDatabase();
        } catch (SQLException e) {
            showMessage("Error: " + e.getMessage(), "red");
        }
    }

    @FXML
    public void handleDeleteTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showMessage("Select a task to delete!", "red"); return; }
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM task WHERE task_id = ?");
            stmt.setInt(1, Integer.parseInt(selected.getTaskId()));
            stmt.executeUpdate();
            showMessage("Task deleted!", "green");
            loadTasksFromDatabase();
        } catch (SQLException e) {
            showMessage("Error deleting: " + e.getMessage(), "red");
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────

    @FXML
    public void goHome() {
        loadScene("/view/home.fxml", "TaskMaster - Home");
    }

    @FXML
    public void openTasks() {
        loadScene("/view/dashboard.fxml", "TaskMaster - My Tasks");
    }

    @FXML
    public void openCalendar() {
        showMessage("Calendar coming soon!", "#a855f7");
    }

    @FXML
    public void openGpa() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/gpa.fxml"));
            javafx.scene.Parent root = loader.load();
            GpaController gpa = loader.getController();
            gpa.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1100, 680);
            getStage().setScene(scene);
            getStage().setTitle("TaskMaster - My Grades");
        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
    }

    @FXML
    public void openStudyTimer() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/study_session.fxml"));
            javafx.scene.Parent root = loader.load();
            StudySessionController sc = loader.getController();
            sc.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1100, 680);
            getStage().setScene(scene);
            getStage().setTitle("TaskMaster - Study Timer");
        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
    }

    @FXML
    public void handleLogout() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 1000, 620);
            getStage().setScene(scene);
            getStage().setTitle("TaskMaster");
        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
    }

    private void loadScene(String fxml, String title) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource(fxml));
            javafx.scene.Parent root = loader.load();
            DashboardController dc = loader.getController();
            dc.currentStudentId    = this.currentStudentId;
            dc.currentStudentName  = this.currentStudentName;
            dc.currentStudentEmail = this.currentStudentEmail;
            dc.reminderShown       = true;
            dc.updateSidebarProfile();
            dc.loadTasksFromDatabase();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 1100, 680);
            getStage().setScene(scene);
            getStage().setTitle(title);
        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
    }

    private javafx.stage.Stage getStage() {
        if (welcomeLabel != null)
            return (javafx.stage.Stage) welcomeLabel.getScene().getWindow();
        if (studentNameLabel != null)
            return (javafx.stage.Stage) studentNameLabel.getScene().getWindow();
        return null;
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
            StringBuilder msg = new StringBuilder();
            final int[] count = {0};
            while (rs.next()) {
                msg.append("• ").append(rs.getString("title"))
                   .append(" — due ").append(rs.getString("deadline")).append("\n");
                count[0]++;
            }
            if (count[0] > 0) {
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Upcoming Deadlines!");
                    alert.setHeaderText(count[0] + " task(s) due in the next 3 days:");
                    alert.setContentText(msg.toString());
                    alert.showAndWait();
                });
            }
        } catch (SQLException e) {
            System.out.println("Error checking deadlines: " + e.getMessage());
        }
    }

    private void clearFields() {
        if (titleField != null)    titleField.clear();
        if (courseField != null)   courseField.clear();
        if (deadlineField != null) deadlineField.clear();
        if (notesField != null)    notesField.clear();
        if (priorityBox != null)   priorityBox.setValue(null);
        if (typeBox != null)       typeBox.setValue(null);
    }

    private void showMessage(String msg, String color) {
        if (messageLabel != null) {
            messageLabel.setText(msg);
            messageLabel.setStyle("-fx-text-fill: " + color + ";");
        }
    }
}