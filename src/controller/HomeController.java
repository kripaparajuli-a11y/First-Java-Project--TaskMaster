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
        // Show reminder popup on login
        javafx.application.Platform.runLater(() -> {
            javafx.stage.Stage stage = getStage();
            ReminderService.checkAndShow(studentId, stage);
        });
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
    @FXML
    public void openReport() {
        try {
            // Gather all stats from DB
            java.sql.Connection conn = database.DatabaseManager.getInstance().getConnection();

            // Task counts
            int total = 0, completed = 0, overdue = 0, inProgress = 0, notStarted = 0;
            java.sql.PreparedStatement ts = conn.prepareStatement(
                "SELECT status, COUNT(*) AS cnt FROM task WHERE student_id=? GROUP BY status");
            ts.setInt(1, currentStudentId);
            java.sql.ResultSet tr = ts.executeQuery();
            while (tr.next()) {
                int c = tr.getInt("cnt"); total += c;
                switch (tr.getString("status")) {
                    case "Completed"   -> completed  = c;
                    case "Overdue"     -> overdue    = c;
                    case "In Progress" -> inProgress = c;
                    case "Not Started" -> notStarted = c;
                }
            }
            tr.close(); ts.close();

            // Study time this week
            java.sql.PreparedStatement ss = conn.prepareStatement(
                "SELECT COALESCE(SUM(duration_min),0) FROM study_session " +
                "WHERE student_id=? AND session_date >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)");
            ss.setInt(1, currentStudentId);
            java.sql.ResultSet sr = ss.executeQuery();
            int weekMins = sr.next() ? sr.getInt(1) : 0;
            sr.close(); ss.close();

            // GPA / overall grade
            java.sql.PreparedStatement gs = conn.prepareStatement(
                "SELECT id FROM course_grade WHERE student_id=?");
            gs.setInt(1, currentStudentId);
            java.sql.ResultSet gr = gs.executeQuery();
            double totalPct = 0; int courseCount = 0;
            while (gr.next()) {
                java.sql.PreparedStatement as = conn.prepareStatement(
                    "SELECT weight, score FROM assessment WHERE course_grade_id=?");
                as.setInt(1, gr.getInt("id"));
                java.sql.ResultSet ar = as.executeQuery();
                double tw = 0, ws = 0;
                while (ar.next()) { double w = ar.getDouble("weight"), sc = ar.getDouble("score"); tw += w; ws += w*sc/100.0; }
                ar.close(); as.close();
                if (tw >= 99.9) { totalPct += ws; courseCount++; }
            }
            gr.close(); gs.close();

            // Upcoming deadlines (next 7 days)
            java.util.List<String> upcoming = new java.util.ArrayList<>();
            java.sql.PreparedStatement us = conn.prepareStatement(
                "SELECT t.title, t.deadline, t.priority FROM task t " +
                "WHERE t.student_id=? AND t.status != 'Completed' " +
                "AND t.deadline BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                "ORDER BY t.deadline ASC LIMIT 5");
            us.setInt(1, currentStudentId);
            java.sql.ResultSet ur = us.executeQuery();
            while (ur.next()) upcoming.add(ur.getString("title") + " — " + ur.getString("deadline") + " [" + ur.getString("priority") + "]");
            ur.close(); us.close();

            // Build final values for lambda
            final int fTotal = total, fCompleted = completed, fOverdue = overdue,
                      fInProgress = inProgress, fNotStarted = notStarted, fWeekMins = weekMins;
            final double fAvgGrade = courseCount > 0 ? Math.round(totalPct / courseCount * 10.0) / 10.0 : -1;
            final int fCourseCount = courseCount;
            final java.util.List<String> fUpcoming = upcoming;

            javafx.application.Platform.runLater(() -> showReportDialog(
                fTotal, fCompleted, fOverdue, fInProgress, fNotStarted,
                fWeekMins, fAvgGrade, fCourseCount, fUpcoming));

        } catch (Exception e) { System.out.println("openReport: " + e.getMessage()); }
    }

    private void showReportDialog(int total, int completed, int overdue, int inProgress,
                                   int notStarted, int weekMins, double avgGrade,
                                   int courseCount, java.util.List<String> upcoming) {

        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog =
            new javafx.scene.control.Dialog<>();
        dialog.setTitle("📊 My Report");
        dialog.setHeaderText(null);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f0ff; -fx-padding: 0;");
        dialog.getDialogPane().setPrefWidth(480);

        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(0);

        // ── Header ──
        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPadding(new javafx.geometry.Insets(18, 22, 16, 22));
        header.setStyle("-fx-background-color: #3b0764;");
        javafx.scene.control.Label icon = new javafx.scene.control.Label("📊");
        icon.setStyle("-fx-font-size: 24px;");
        javafx.scene.layout.VBox ht = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label htitle = new javafx.scene.control.Label("Progress Report");
        htitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: white;");
        javafx.scene.control.Label hsub = new javafx.scene.control.Label(
            currentStudentName + "  ·  " +
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")));
        hsub.setStyle("-fx-font-size: 11px; -fx-text-fill: #e9d5ff;");
        ht.getChildren().addAll(htitle, hsub);
        header.getChildren().addAll(icon, ht);

        // ── Body ──
        javafx.scene.layout.VBox body = new javafx.scene.layout.VBox(14);
        body.setPadding(new javafx.geometry.Insets(18, 22, 10, 22));

        // Task summary section
        javafx.scene.control.Label taskTitle = sectionLabel("✅  Task Summary");
        int pct = total > 0 ? (completed * 100 / total) : 0;

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
        grid.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e9d5ff; -fx-border-radius: 10; -fx-border-width: 1;");

        grid.add(statRow("Total Tasks",    String.valueOf(total),      "#3b0764"), 0, 0);
        grid.add(statRow("Completed",      completed + "  (" + pct + "%)", "#15803d"), 0, 1);
        grid.add(statRow("In Progress",    String.valueOf(inProgress), "#b45309"), 0, 2);
        grid.add(statRow("Not Started",    String.valueOf(notStarted), "#6d28d9"), 0, 3);
        grid.add(statRow("Overdue",        String.valueOf(overdue),    "#b91c1c"), 0, 4);

        // Progress bar
        javafx.scene.layout.HBox barBox = new javafx.scene.layout.HBox(8);
        barBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label barLbl = new javafx.scene.control.Label("Completion:");
        barLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #3b0764; -fx-font-weight: bold;");
        javafx.scene.layout.Region barBg = new javafx.scene.layout.Region();
        barBg.setPrefSize(200, 10); barBg.setMaxWidth(200);
        barBg.setStyle("-fx-background-color: #e9d5ff; -fx-background-radius: 5;");
        javafx.scene.layout.Region barFill = new javafx.scene.layout.Region();
        barFill.setPrefHeight(10);
        barFill.setPrefWidth(pct * 2.0);
        barFill.setStyle("-fx-background-color: #a855f7; -fx-background-radius: 5;");
        javafx.scene.layout.StackPane barStack = new javafx.scene.layout.StackPane(barBg, barFill);
        javafx.scene.layout.StackPane.setAlignment(barFill, javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label pctLbl = new javafx.scene.control.Label(pct + "%");
        pctLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #581c87;");
        barBox.getChildren().addAll(barLbl, barStack, pctLbl);
        grid.add(barBox, 0, 5);

        // Study time section
        javafx.scene.control.Label studyTitle = sectionLabel("⏱  Study Time (This Week)");
        String studyStr = weekMins >= 60
            ? (weekMins / 60) + "h " + (weekMins % 60) + "m studied this week"
            : weekMins + " minutes studied this week";
        javafx.scene.control.Label studyLbl = new javafx.scene.control.Label(studyStr);
        studyLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1d4ed8;" +
            "-fx-background-color: #eff6ff; -fx-background-radius: 8; -fx-padding: 10 14;");
        studyLbl.setMaxWidth(Double.MAX_VALUE);

        // GPA section
        javafx.scene.control.Label gradeTitle = sectionLabel("🎓  Academic Performance");
        String gradeStr = courseCount > 0
            ? String.format("Overall Grade: %.1f%%  (%d completed course%s)",
                avgGrade, courseCount, courseCount > 1 ? "s" : "")
            : "No completed courses yet — add grades in My Grades";
        javafx.scene.control.Label gradeLbl = new javafx.scene.control.Label(gradeStr);
        gradeLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" +
            "-fx-text-fill: " + (courseCount > 0 ? "#15803d" : "#9e9e9e") + ";" +
            "-fx-background-color: " + (courseCount > 0 ? "#f0fdf4" : "#f5f5f5") + ";" +
            "-fx-background-radius: 8; -fx-padding: 10 14;");
        gradeLbl.setMaxWidth(Double.MAX_VALUE);
        gradeLbl.setWrapText(true);

        // Upcoming deadlines section
        javafx.scene.control.Label upTitle = sectionLabel("📅  Upcoming Deadlines (Next 7 Days)");
        javafx.scene.layout.VBox upBox = new javafx.scene.layout.VBox(6);
        upBox.setPadding(new javafx.geometry.Insets(8, 14, 8, 14));
        upBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e9d5ff; -fx-border-radius: 10; -fx-border-width: 1;");
        if (upcoming.isEmpty()) {
            javafx.scene.control.Label none = new javafx.scene.control.Label("🎉  No deadlines in the next 7 days!");
            none.setStyle("-fx-font-size: 12px; -fx-text-fill: #15803d;");
            upBox.getChildren().add(none);
        } else {
            for (String item : upcoming) {
                javafx.scene.control.Label row = new javafx.scene.control.Label("• " + item);
                row.setStyle("-fx-font-size: 11px; -fx-text-fill: #3b0764;");
                upBox.getChildren().add(row);
            }
        }

        body.getChildren().addAll(taskTitle, grid, studyTitle, studyLbl, gradeTitle, gradeLbl, upTitle, upBox);

        // ── Close button ──
        javafx.scene.layout.HBox footer = new javafx.scene.layout.HBox();
        footer.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        footer.setPadding(new javafx.geometry.Insets(10, 22, 18, 22));
        javafx.scene.control.ButtonType closeBtn =
            new javafx.scene.control.ButtonType("Close", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeBtn);
        javafx.scene.control.Button close =
            (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(closeBtn);
        close.setStyle("-fx-background-color: #a855f7; -fx-text-fill: white; -fx-font-size: 13px;" +
            "-fx-font-weight: bold; -fx-padding: 10 28; -fx-background-radius: 8; -fx-cursor: hand;");

        root.getChildren().addAll(header, body);
        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    private javafx.scene.control.Label sectionLabel(String text) {
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3b0764;");
        return lbl;
    }

    private javafx.scene.layout.HBox statRow(String label, String value, String color) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        javafx.scene.control.Label lbl = new javafx.scene.control.Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");
        lbl.setPrefWidth(110);
        javafx.scene.control.Label val = new javafx.scene.control.Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        row.getChildren().addAll(lbl, val);
        return row;
    }
}