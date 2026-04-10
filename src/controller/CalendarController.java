package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import database.DatabaseManager;
import model.Task;
import java.sql.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class CalendarController {

    @FXML private Label monthYearLabel;
    @FXML private GridPane calendarGrid;
    @FXML private Label welcomeLabel;
    @FXML private Label studentNameLabel;
    @FXML private Label studentEmailLabel;
    @FXML private Label avatarInitial;

    @FXML private Label dayDetailLabel;
    @FXML private Label dayTasksLabel;

    @FXML private HBox notStartedRow;
    @FXML private HBox inProgressRow;
    @FXML private HBox completedRow;
    @FXML private HBox overdueRow;

    private int currentStudentId = 1;
    private String currentStudentName = "Student";
    private String currentStudentEmail = "";
    private YearMonth currentYearMonth = YearMonth.now();
    private Map<LocalDate, List<String>> taskMap = new HashMap<>();
    private List<Task> allTasks = new ArrayList<>();

    public void setStudentInfo(String name, String email, int studentId) {
        this.currentStudentName  = name;
        this.currentStudentEmail = email;
        this.currentStudentId    = studentId;
        updateSidebarProfile();
        loadTasksFromDatabase();
        buildCalendar();
        buildKanban();
    }

    private void updateSidebarProfile() {
        if (studentNameLabel  != null) studentNameLabel.setText(currentStudentName);
        if (studentEmailLabel != null) studentEmailLabel.setText(currentStudentEmail);
        if (welcomeLabel      != null) welcomeLabel.setText(currentStudentName + "!");
        if (avatarInitial     != null && !currentStudentName.isEmpty())
            avatarInitial.setText(String.valueOf(currentStudentName.charAt(0)).toUpperCase());
    }

    @FXML
    public void initialize() {}

    private void loadTasksFromDatabase() {
        taskMap.clear();
        allTasks.clear();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            String sql = "SELECT t.task_id, t.title, t.description, t.deadline, " +
                         "t.priority, t.status, t.task_type, c.course_name " +
                         "FROM task t LEFT JOIN course c ON t.course_id = c.course_id " +
                         "WHERE t.student_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String deadlineStr = rs.getString("deadline");
                Task task = new Task(
                    String.valueOf(rs.getInt("task_id")),
                    rs.getString("title"),
                    rs.getString("description"),
                    deadlineStr,
                    rs.getString("priority"),
                    rs.getString("status"),
                    rs.getString("course_name"),
                    String.valueOf(currentStudentId),
                    rs.getString("task_type")
                );
                allTasks.add(task);
                if (deadlineStr != null) {
                    try {
                        LocalDate date = LocalDate.parse(deadlineStr.length() > 10
                            ? deadlineStr.substring(0, 10) : deadlineStr);
                        taskMap.computeIfAbsent(date, k -> new ArrayList<>()).add(task.getTitle());
                    } catch (Exception ignored) {}
                }
            }
        } catch (SQLException e) {
            System.out.println("Error loading tasks: " + e.getMessage());
        }
    }

    private void buildCalendar() {
        calendarGrid.getChildren().clear();

        monthYearLabel.setText(
            currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
            + " " + currentYearMonth.getYear());

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label h = new Label(days[i]);
            h.setMaxWidth(Double.MAX_VALUE);
            h.setAlignment(Pos.CENTER);
            h.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; " +
                       "-fx-text-fill: #a855f7; -fx-padding: 4 0;");
            calendarGrid.add(h, i, 0);
        }

        LocalDate firstDay  = currentYearMonth.atDay(1);
        int startCol        = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth     = currentYearMonth.lengthOfMonth();
        LocalDate today     = LocalDate.now();
        int row = 1, col = startCol;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentYearMonth.atDay(day);
            List<String> tasks = taskMap.getOrDefault(date, new ArrayList<>());

            VBox cell = new VBox(1);
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setPrefHeight(62);

            String cellStyle, numStyle;
            if (date.equals(today)) {
                cellStyle = "-fx-background-color: #a855f7; -fx-background-radius: 8; -fx-padding: 4;";
                numStyle  = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;";
            } else if (!tasks.isEmpty()) {
                cellStyle = "-fx-background-color: #f3e5f5; -fx-background-radius: 8; -fx-padding: 4;" +
                            "-fx-border-color: #ce93d8; -fx-border-radius: 8; -fx-border-width: 1;";
                numStyle  = "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3b0764;";
            } else {
                cellStyle = "-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 4;" +
                            "-fx-border-color: #ede7f6; -fx-border-radius: 8; -fx-border-width: 1;";
                numStyle  = "-fx-font-size: 11px; -fx-text-fill: #555;";
            }

            cell.setStyle(cellStyle);
            Label dayNum = new Label(String.valueOf(day));
            dayNum.setStyle(numStyle);
            cell.getChildren().add(dayNum);

            if (!tasks.isEmpty()) {
                Label dot = new Label("●");
                String dotColor = date.equals(today) ? "white" : "#a855f7";
                dot.setStyle("-fx-font-size: 7px; -fx-text-fill: " + dotColor + ";");
                cell.getChildren().add(dot);
                if (tasks.size() > 1) {
                    Label more = new Label("+" + tasks.size());
                    more.setStyle("-fx-font-size: 7px; -fx-text-fill: " +
                                  (date.equals(today) ? "white" : "#581c87") + ";");
                    cell.getChildren().add(more);
                }
            }

            final String fStyle = cellStyle;
            final LocalDate fd  = date;
            final List<String> ft = new ArrayList<>(tasks);

            cell.setOnMouseClicked(e -> showDayDetail(fd, ft));
            cell.setOnMouseEntered(e -> {
                if (!fd.equals(today))
                    cell.setStyle(fStyle + "-fx-effect: dropshadow(gaussian, #ce93d8, 4, 0, 0, 0);");
            });
            cell.setOnMouseExited(e -> cell.setStyle(fStyle));

            calendarGrid.add(cell, col, row);
            if (++col == 7) { col = 0; row++; }
        }
    }

    private void showDayDetail(LocalDate date, List<String> tasks) {
        dayDetailLabel.setText("📅  " +
            date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        if (tasks.isEmpty()) {
            dayTasksLabel.setText("No tasks due on this day.");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tasks.size(); i++)
                sb.append(i + 1).append(". ").append(tasks.get(i)).append("\n");
            dayTasksLabel.setText(sb.toString().trim());
        }
    }

    private void buildKanban() {
        notStartedRow.getChildren().clear();
        inProgressRow.getChildren().clear();
        completedRow.getChildren().clear();
        overdueRow.getChildren().clear();

        for (Task task : allTasks) {
            VBox card = buildCard(task);
            switch (task.getStatus()) {
                case "Not Started" -> notStartedRow.getChildren().add(card);
                case "In Progress" -> inProgressRow.getChildren().add(card);
                case "Completed"   -> completedRow.getChildren().add(card);
                case "Overdue"     -> overdueRow.getChildren().add(card);
                default            -> notStartedRow.getChildren().add(card);
            }
        }

        addEmpty(notStartedRow);
        addEmpty(inProgressRow);
        addEmpty(completedRow);
        addEmpty(overdueRow);
    }

    private VBox buildCard(Task task) {
        VBox card = new VBox(4);
        card.setPrefWidth(155);
        card.setMaxWidth(155);

        String pColor = switch (task.getPriority() == null ? "" : task.getPriority()) {
            case "High"   -> "#ef4444";
            case "Medium" -> "#f59e0b";
            default       -> "#22c55e";
        };

        card.setStyle(
            "-fx-background-color: #f5f0ff; -fx-background-radius: 8;" +
            "-fx-padding: 8 10;" +
            "-fx-border-color: " + pColor + " #e9d5ff #e9d5ff #e9d5ff;" +
            "-fx-border-radius: 8; -fx-border-width: 3 1 1 1;");

        Label title = new Label(task.getTitle());
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #3b0764;");
        title.setWrapText(true);
        title.setMaxWidth(135);

        HBox meta = new HBox(5);
        meta.setAlignment(Pos.CENTER_LEFT);

        String courseName = task.getCourseId() != null ? task.getCourseId() : "—";
        Label course = new Label(courseName);
        course.setStyle("-fx-font-size: 9px; -fx-text-fill: #a855f7;" +
                        "-fx-background-color: #ede7f6; -fx-background-radius: 4; -fx-padding: 1 4;");

        Label pLabel = new Label(task.getPriority() != null ? task.getPriority() : "—");
        pLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: white;" +
                        "-fx-background-color: " + pColor + ";" +
                        "-fx-background-radius: 4; -fx-padding: 1 4;");

        meta.getChildren().addAll(course, pLabel);

        Label deadline = new Label("📅 " + (task.getDeadline() != null ? task.getDeadline() : "—"));
        deadline.setStyle("-fx-font-size: 9px; -fx-text-fill: #888;");

        card.getChildren().addAll(title, meta, deadline);
        return card;
    }

    private void addEmpty(HBox row) {
        if (row.getChildren().isEmpty()) {
            Label lbl = new Label("No tasks");
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #bbb; -fx-padding: 6 0;");
            row.getChildren().add(lbl);
        }
    }

    @FXML public void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        loadTasksFromDatabase(); buildCalendar();
    }

    @FXML public void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        loadTasksFromDatabase(); buildCalendar();
    }

    @FXML public void goHome()       { navigate("/view/home.fxml", "TaskMaster - Home"); }
    @FXML public void openTasks()    { navigate("/view/dashboard.fxml", "TaskMaster - My Tasks"); }
    @FXML public void openCalendar() { loadTasksFromDatabase(); buildCalendar(); buildKanban(); }

    @FXML public void openGpa() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/gpa.fxml"));
            javafx.scene.Parent root = loader.load();
            GpaController gpa = loader.getController();
            gpa.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - My Grades");
        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
    }

    @FXML public void openStudyTimer() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/study_session.fxml"));
            javafx.scene.Parent root = loader.load();
            StudySessionController sc = loader.getController();
            sc.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - Study Timer");
        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
    }

    @FXML public void handleLogout() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/login.fxml"));
            getStage().setScene(new javafx.scene.Scene(loader.load(), 1000, 620));
            getStage().setTitle("TaskMaster");
        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
    }

    private void navigate(String fxml, String title) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource(fxml));
            javafx.scene.Parent root = loader.load();
            if (fxml.contains("home")) {
                HomeController hc = loader.getController();
                hc.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            } else {
                DashboardController dc = loader.getController();
                dc.setReminderShown(true);
                dc.setStudentInfo(currentStudentName, currentStudentId, currentStudentEmail);
            }
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle(title);
        } catch (Exception e) { System.out.println("Error navigating: " + e.getMessage()); }
    }

    private javafx.stage.Stage getStage() {
        if (studentNameLabel != null)
            return (javafx.stage.Stage) studentNameLabel.getScene().getWindow();
        if (monthYearLabel != null)
            return (javafx.stage.Stage) monthYearLabel.getScene().getWindow();
        return null;
    }
}