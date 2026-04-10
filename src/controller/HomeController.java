package controller;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.application.Platform;
import database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public class HomeController {

    // ── Sidebar ───────────────────────────────────────────────────────────
    @FXML private Label studentNameLabel;
    @FXML private Label studentEmailLabel;
    @FXML private Label avatarInitial;

    // ── Header ────────────────────────────────────────────────────────────
    @FXML private Label greetingLabel;
    @FXML private Label welcomeLabel;
    @FXML private Label dateLabel;
    @FXML private Label studyTodayLabel;

    // ── Stat cards ────────────────────────────────────────────────────────
    @FXML private Label totalTasksLabel;
    @FXML private Label totalTasksSubLabel;
    @FXML private Label completedLabel;
    @FXML private Label completedPctLabel;
    @FXML private Label overdueLabel;
    @FXML private Label inProgressLabel;
    @FXML private Label totalStudyLabel;

    // ── Charts ────────────────────────────────────────────────────────────
    @FXML private Canvas donutCanvas;
    @FXML private Canvas barCanvas;
    @FXML private Label  weekTotalLabel;

    // ── Upcoming deadlines ────────────────────────────────────────────────
    @FXML private VBox  upcomingList;
    @FXML private Label noUpcomingLabel;

    // ── Priority bars ─────────────────────────────────────────────────────
    @FXML private Label  highPriorityLabel;
    @FXML private Label  medPriorityLabel;
    @FXML private Label  lowPriorityLabel;
    @FXML private Region highBar;
    @FXML private Region medBar;
    @FXML private Region lowBar;
    @FXML private Label  overallPctLabel;
    @FXML private Region overallBar;

    // ── GPA ───────────────────────────────────────────────────────────────
    @FXML private Label gpaLabel;

    // ── State ─────────────────────────────────────────────────────────────
    private int    currentStudentId    = 1;
    private String currentStudentName  = "Student";
    private String currentStudentEmail = "";

    // ─────────────────────────────────────────────────────────────────────

    public void setStudentInfo(String name, String email, int studentId) {
        this.currentStudentName  = name;
        this.currentStudentEmail = email;
        this.currentStudentId    = studentId;
        refreshAll();
    }

    @FXML
    public void initialize() {
        updateDateGreeting();
    }

    private void refreshAll() {
        updateSidebarProfile();
        updateDateGreeting();
        loadTaskStats();
        loadStudyStats();
        loadUpcomingDeadlines();
        loadGpa();
    }

    // ── Sidebar ───────────────────────────────────────────────────────────

    private void updateSidebarProfile() {
        if (studentNameLabel  != null) studentNameLabel.setText(currentStudentName);
        if (studentEmailLabel != null) studentEmailLabel.setText(currentStudentEmail);
        if (avatarInitial     != null && !currentStudentName.isEmpty())
            avatarInitial.setText(String.valueOf(currentStudentName.charAt(0)).toUpperCase());
    }

    // ── Date / Greeting ───────────────────────────────────────────────────

    private void updateDateGreeting() {
        LocalDate today = LocalDate.now();
        if (dateLabel != null)
            dateLabel.setText(today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy")));

        int hour = java.time.LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning," : hour < 17 ? "Good afternoon," : "Good evening,";
        if (greetingLabel != null) greetingLabel.setText(greeting);
        if (welcomeLabel  != null) welcomeLabel.setText(currentStudentName + "! 👋");
    }

    // ── Task Stats ────────────────────────────────────────────────────────

    private void loadTaskStats() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            PreparedStatement stmt = conn.prepareStatement(
                "SELECT status, COUNT(*) AS cnt FROM task WHERE student_id=? GROUP BY status");
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            int total = 0, completed = 0, overdue = 0, inProgress = 0, notStarted = 0;
            while (rs.next()) {
                int cnt = rs.getInt("cnt");
                total += cnt;
                switch (rs.getString("status")) {
                    case "Completed"   -> completed  = cnt;
                    case "Overdue"     -> overdue    = cnt;
                    case "In Progress" -> inProgress = cnt;
                    case "Not Started" -> notStarted = cnt;
                }
            }
            rs.close(); stmt.close();

            PreparedStatement p = conn.prepareStatement(
                "SELECT priority, COUNT(*) AS cnt FROM task WHERE student_id=? GROUP BY priority");
            p.setInt(1, currentStudentId);
            ResultSet pr = p.executeQuery();
            int high = 0, med = 0, low = 0;
            while (pr.next()) {
                int cnt = pr.getInt("cnt");
                switch (pr.getString("priority")) {
                    case "High"   -> high = cnt;
                    case "Medium" -> med  = cnt;
                    case "Low"    -> low  = cnt;
                }
            }
            pr.close(); p.close();

            final int fTotal = total, fCompleted = completed, fOverdue = overdue,
                      fInProgress = inProgress, fHigh = high, fMed = med, fLow = low,
                      fNotStarted = notStarted;

            Platform.runLater(() -> {
                int pct = fTotal > 0 ? (fCompleted * 100 / fTotal) : 0;

                if (totalTasksLabel    != null) totalTasksLabel.setText(String.valueOf(fTotal));
                if (totalTasksSubLabel != null) totalTasksSubLabel.setText(
                    fTotal == 1 ? "1 task total" : fTotal + " tasks total");
                if (completedLabel   != null) completedLabel.setText(String.valueOf(fCompleted));
                if (completedPctLabel!= null) completedPctLabel.setText(pct + "% done");
                if (overdueLabel     != null) overdueLabel.setText(String.valueOf(fOverdue));
                if (inProgressLabel  != null) inProgressLabel.setText(String.valueOf(fInProgress));

                int maxPri = Math.max(1, Math.max(fHigh, Math.max(fMed, fLow)));
                if (highPriorityLabel != null) highPriorityLabel.setText(String.valueOf(fHigh));
                if (medPriorityLabel  != null) medPriorityLabel.setText(String.valueOf(fMed));
                if (lowPriorityLabel  != null) lowPriorityLabel.setText(String.valueOf(fLow));
                if (highBar != null) highBar.setMaxWidth(180.0 * fHigh / maxPri);
                if (medBar  != null) medBar.setMaxWidth(180.0 * fMed  / maxPri);
                if (lowBar  != null) lowBar.setMaxWidth(180.0 * fLow  / maxPri);

                if (overallPctLabel != null) overallPctLabel.setText(pct + "%");
                if (overallBar      != null) overallBar.setMaxWidth(180.0 * pct / 100.0);

                drawDonut(fCompleted, fOverdue, fInProgress, fNotStarted);
            });

        } catch (SQLException e) {
            System.out.println("HomeController.loadTaskStats: " + e.getMessage());
        }
    }

    // ── Study Stats ───────────────────────────────────────────────────────

    private void loadStudyStats() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            PreparedStatement t = conn.prepareStatement(
                "SELECT COALESCE(SUM(duration_min),0) FROM study_session " +
                "WHERE student_id=? AND session_date=CURDATE()");
            t.setInt(1, currentStudentId);
            ResultSet tr = t.executeQuery();
            int todayMins = tr.next() ? tr.getInt(1) : 0;
            tr.close(); t.close();

            PreparedStatement w = conn.prepareStatement(
                "SELECT COALESCE(SUM(duration_min),0) FROM study_session " +
                "WHERE student_id=? AND session_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)");
            w.setInt(1, currentStudentId);
            ResultSet wr = w.executeQuery();
            int weekMins = wr.next() ? wr.getInt(1) : 0;
            wr.close(); w.close();

            int[] dayMins = new int[7];
            String[] dayLabels = new String[7];
            LocalDate today = LocalDate.now();
            for (int i = 6; i >= 0; i--) {
                LocalDate d = today.minusDays(i);
                dayLabels[6 - i] = d.getDayOfWeek()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                PreparedStatement ds = conn.prepareStatement(
                    "SELECT COALESCE(SUM(duration_min),0) FROM study_session " +
                    "WHERE student_id=? AND session_date=?");
                ds.setInt(1, currentStudentId);
                ds.setString(2, d.toString());
                ResultSet dr = ds.executeQuery();
                dayMins[6 - i] = dr.next() ? dr.getInt(1) : 0;
                dr.close(); ds.close();
            }

            final int fToday = todayMins, fWeek = weekMins;
            final int[] fDayMins = dayMins;
            final String[] fLabels = dayLabels;

            Platform.runLater(() -> {
                String todayStr = fToday >= 60
                    ? (fToday / 60) + "h " + (fToday % 60) + "m studied today"
                    : fToday + " min studied today";
                if (studyTodayLabel != null) studyTodayLabel.setText(todayStr);

                String weekStr = fWeek >= 60
                    ? (fWeek / 60) + "h " + (fWeek % 60) + "m"
                    : fWeek + "m";
                if (totalStudyLabel != null) totalStudyLabel.setText(weekStr);
                if (weekTotalLabel  != null) weekTotalLabel.setText(fWeek + " min total");

                drawBarChart(fDayMins, fLabels);
            });

        } catch (SQLException e) {
            System.out.println("HomeController.loadStudyStats: " + e.getMessage());
        }
    }

    // ── Upcoming Deadlines ────────────────────────────────────────────────

    private void loadUpcomingDeadlines() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT t.title, t.deadline, t.priority, c.course_name " +
                "FROM task t LEFT JOIN course c ON t.course_id=c.course_id " +
                "WHERE t.student_id=? AND t.status != 'Completed' " +
                "AND t.deadline BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                "ORDER BY t.deadline ASC LIMIT 5");
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            java.util.List<String[]> items = new java.util.ArrayList<>();
            while (rs.next()) {
                items.add(new String[]{
                    rs.getString("title"),
                    rs.getString("deadline"),
                    rs.getString("priority"),
                    rs.getString("course_name") != null ? rs.getString("course_name") : ""
                });
            }
            rs.close(); stmt.close();

            Platform.runLater(() -> {
                if (upcomingList == null) return;
                upcomingList.getChildren().clear();
                if (items.isEmpty()) {
                    if (noUpcomingLabel != null) {
                        noUpcomingLabel.setVisible(true);
                        noUpcomingLabel.setManaged(true);
                    }
                    return;
                }
                if (noUpcomingLabel != null) {
                    noUpcomingLabel.setVisible(false);
                    noUpcomingLabel.setManaged(false);
                }
                for (String[] item : items) {
                    String title    = item[0];
                    String deadline = item[1];
                    String priority = item[2];
                    String course   = item[3];

                    // Colour by priority — light theme colours
                    String dotColor = switch (priority) {
                        case "High"   -> "#b91c1c";
                        case "Medium" -> "#b45309";
                        default       -> "#15803d";
                    };
                    String rowBg = switch (priority) {
                        case "High"   -> "#fff1f2";
                        case "Medium" -> "#fffbeb";
                        default       -> "#f0fdf4";
                    };

                    long daysLeft = 0;
                    try {
                        daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                            LocalDate.now(), LocalDate.parse(deadline));
                    } catch (Exception ignored) {}

                    String daysStr = daysLeft == 0 ? "Today!"
                        : daysLeft == 1 ? "Tomorrow"
                        : "in " + daysLeft + " days";

                    HBox row = new HBox(10);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setStyle("-fx-background-color:" + rowBg + ";" +
                                 "-fx-background-radius:8;-fx-padding:10 12;" +
                                 "-fx-border-color:#e9d5ff;-fx-border-radius:8;-fx-border-width:1;");

                    Region dot = new Region();
                    dot.setPrefSize(8, 8);
                    dot.setMinSize(8, 8);
                    dot.setMaxSize(8, 8);
                    dot.setStyle("-fx-background-color:" + dotColor + ";-fx-background-radius:4;");

                    VBox info = new VBox(2);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    Label titleLbl = new Label(title);
                    titleLbl.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#3b0764;");
                    Label courseLbl = new Label(course.isEmpty() ? deadline : course + " · " + deadline);
                    courseLbl.setStyle("-fx-font-size:10px;-fx-text-fill:#a855f7;");
                    info.getChildren().addAll(titleLbl, courseLbl);

                    Label dayLbl = new Label(daysStr);
                    dayLbl.setStyle("-fx-font-size:10px;-fx-font-weight:bold;-fx-text-fill:" + dotColor + ";");

                    row.getChildren().addAll(dot, info, dayLbl);
                    upcomingList.getChildren().add(row);
                }
            });

        } catch (SQLException e) {
            System.out.println("HomeController.loadUpcoming: " + e.getMessage());
        }
    }

    // ── GPA ───────────────────────────────────────────────────────────────

    private void loadGpa() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT AVG(gpa_value) FROM gpa WHERE student_id=?");
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double gpa = rs.getDouble(1);
                if (!rs.wasNull()) {
                    final String gpaStr = String.format("%.2f", gpa);
                    Platform.runLater(() -> { if (gpaLabel != null) gpaLabel.setText(gpaStr); });
                }
            }
            rs.close(); stmt.close();
        } catch (SQLException e) {
            System.out.println("HomeController.loadGpa: " + e.getMessage());
        }
    }

    // ── Donut Chart — light theme colours ────────────────────────────────

    private void drawDonut(int completed, int overdue, int inProgress, int notStarted) {
        if (donutCanvas == null) return;
        GraphicsContext gc = donutCanvas.getGraphicsContext2D();
        double w = donutCanvas.getWidth();
        double h = donutCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        int total = completed + overdue + inProgress + notStarted;
        if (total == 0) {
            gc.setStroke(Color.web("#e9d5ff"));
            gc.setLineWidth(22);
            gc.strokeOval(w / 2 - 70, h / 2 - 70, 140, 140);
            gc.setFill(Color.web("#a855f7"));
            gc.setFont(javafx.scene.text.Font.font("System", 12));
            gc.fillText("No tasks yet", w / 2 - 36, h / 2 + 5);
            return;
        }

        double[] values = {completed, overdue, inProgress, notStarted};
        // Light-theme friendly colours matching the stat cards
        String[] colors = {"#15803d", "#b91c1c", "#b45309", "#6d28d9"};

        double cx = w / 2, cy = h / 2;
        double radius = 70, innerR = 48;
        double startAngle = -Math.PI / 2;

        for (int i = 0; i < values.length; i++) {
            if (values[i] == 0) continue;
            double sweep = (values[i] / total) * 2 * Math.PI;

            gc.setFill(Color.web(colors[i]));
            gc.beginPath();
            gc.arc(cx, cy, radius, radius, Math.toDegrees(-startAngle),
                   -Math.toDegrees(sweep));
            gc.arc(cx, cy, innerR, innerR,
                   Math.toDegrees(-(startAngle + sweep)),
                   Math.toDegrees(sweep));
            gc.closePath();
            gc.fill();

            startAngle += sweep;
        }

        // Centre text — dark on light background
        int pct = total > 0 ? (completed * 100 / total) : 0;
        gc.setFill(Color.web("#3b0764"));
        gc.setFont(javafx.scene.text.Font.font("System Bold", 22));
        String pctStr = pct + "%";
        gc.fillText(pctStr, cx - (pctStr.length() * 7), cy + 5);
        gc.setFill(Color.web("#a855f7"));
        gc.setFont(javafx.scene.text.Font.font("System", 10));
        gc.fillText("complete", cx - 24, cy + 20);
    }

    // ── Bar Chart — light theme colours ──────────────────────────────────

    private void drawBarChart(int[] dayMins, String[] labels) {
        if (barCanvas == null) return;
        GraphicsContext gc = barCanvas.getGraphicsContext2D();
        double w = barCanvas.getWidth();
        double h = barCanvas.getHeight();
        gc.clearRect(0, 0, w, h);

        int max = 1;
        for (int m : dayMins) if (m > max) max = m;

        double padLeft = 36, padBottom = 28, padTop = 14;
        double chartH = h - padBottom - padTop;
        double barW = (w - padLeft - 20) / 7.0;
        double gap   = barW * 0.25;
        double bw    = barW - gap;

        // Y-axis grid lines
        gc.setStroke(Color.web("#e9d5ff"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = padTop + chartH - (chartH * i / 4.0);
            gc.strokeLine(padLeft, y, w - 10, y);
            gc.setFill(Color.web("#a855f7"));
            gc.setFont(javafx.scene.text.Font.font("System", 9));
            int val = max * i / 4;
            gc.fillText(val + "m", 0, y + 3);
        }

        // Bars
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            double x    = padLeft + i * barW + gap / 2;
            double barH = dayMins[i] == 0 ? 2 : (chartH * dayMins[i] / (double) max);
            double y    = padTop + chartH - barH;

            boolean isToday = today.minusDays(6 - i).equals(today);

            // Bar fill — purple gradient, today brighter
            LinearGradient grad = new LinearGradient(0, y, 0, y + barH, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(isToday ? "#a855f7" : "#c084fc")),
                new Stop(1, Color.web(isToday ? "#6d28d9" : "#e9d5ff")));
            gc.setFill(grad);

            double r = Math.min(4, bw / 2);
            gc.beginPath();
            gc.moveTo(x + r, y);
            gc.lineTo(x + bw - r, y);
            gc.arcTo(x + bw, y, x + bw, y + r, r);
            gc.lineTo(x + bw, y + barH);
            gc.lineTo(x, y + barH);
            gc.arcTo(x, y, x + r, y, r);
            gc.closePath();
            gc.fill();

            // Value label on top
            if (dayMins[i] > 0) {
                gc.setFill(Color.web("#581c87"));
                gc.setFont(javafx.scene.text.Font.font("System", 9));
                String valStr = dayMins[i] >= 60
                    ? (dayMins[i] / 60) + "h"
                    : dayMins[i] + "m";
                gc.fillText(valStr, x + bw / 2 - valStr.length() * 3, y - 3);
            }

            // Day label
            gc.setFill(isToday ? Color.web("#581c87") : Color.web("#a855f7"));
            gc.setFont(javafx.scene.text.Font.font(isToday ? "System Bold" : "System", 10));
            gc.fillText(labels[i], x + bw / 2 - labels[i].length() * 3,
                        padTop + chartH + 16);
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────

    /** Called when the Home button is clicked from any other screen navigating back here. */
    @FXML public void goHome() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/home.fxml"));
            javafx.scene.Parent root = loader.load();
            HomeController hc = loader.getController();
            hc.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - Home");
        } catch (Exception e) { System.out.println("goHome: " + e.getMessage()); }
    }

    @FXML public void openTasks() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/dashboard.fxml"));
            javafx.scene.Parent root = loader.load();
            DashboardController dc = loader.getController();
            dc.setReminderShown(true);
            dc.setStudentInfo(currentStudentName, currentStudentId, currentStudentEmail);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - My Tasks");
        } catch (Exception e) { System.out.println("openTasks: " + e.getMessage()); }
    }

    @FXML public void openCalendar() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/calendar.fxml"));
            javafx.scene.Parent root = loader.load();
            CalendarController cal = loader.getController();
            cal.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - Calendar");
        } catch (Exception e) { System.out.println("openCalendar: " + e.getMessage()); }
    }

    @FXML public void openGpa() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/gpa.fxml"));
            javafx.scene.Parent root = loader.load();
            GpaController gpa = loader.getController();
            gpa.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - My Grades");
        } catch (Exception e) { System.out.println("openGpa: " + e.getMessage()); }
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
        } catch (Exception e) { System.out.println("openStudyTimer: " + e.getMessage()); }
    }

    @FXML public void handleLogout() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/login.fxml"));
            getStage().setScene(new javafx.scene.Scene(loader.load(), 1000, 620));
            getStage().setTitle("TaskMaster");
        } catch (Exception e) { System.out.println("logout: " + e.getMessage()); }
    }

    private javafx.stage.Stage getStage() {
        if (studentNameLabel != null)
            return (javafx.stage.Stage) studentNameLabel.getScene().getWindow();
        return null;
    }
}