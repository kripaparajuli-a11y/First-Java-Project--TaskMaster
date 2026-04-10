package controller;

import database.DatabaseManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * ReminderService — Observer pattern implementation.
 * Checks upcoming deadlines on login and shows a styled popup
 * categorised by urgency: Today, 1 day, 3 days, and 7 days.
 */
public class ReminderService {

    // ── Inner record to hold a reminder entry ────────────────────────────

    private static class ReminderEntry {
        String title;
        String course;
        String deadline;
        String priority;
        long   daysLeft;

        ReminderEntry(String title, String course,
                      String deadline, String priority, long daysLeft) {
            this.title    = title;
            this.course   = course;
            this.deadline = deadline;
            this.priority = priority;
            this.daysLeft = daysLeft;
        }
    }

    // ── Main entry point ─────────────────────────────────────────────────

    /**
     * Called once on login. Fetches tasks due within 7 days and,
     * if any exist, shows a styled reminder dialog.
     */
    public static void checkAndShow(int studentId, Stage ownerStage) {
        // Run DB query on background thread
        new Thread(() -> {
            List<ReminderEntry> entries = fetchUpcoming(studentId);
            if (!entries.isEmpty()) {
                Platform.runLater(() -> showReminderDialog(entries, ownerStage));
            }
        }).start();
    }

    // ── Database fetch ────────────────────────────────────────────────────

    private static List<ReminderEntry> fetchUpcoming(int studentId) {
        List<ReminderEntry> list = new ArrayList<>();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT t.title, t.deadline, t.priority, c.course_name " +
                "FROM task t LEFT JOIN course c ON t.course_id = c.course_id " +
                "WHERE t.student_id = ? " +
                "  AND t.status != 'Completed' " +
                "  AND t.deadline BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) " +
                "ORDER BY t.deadline ASC");
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            LocalDate today = LocalDate.now();
            while (rs.next()) {
                String deadline = rs.getString("deadline");
                long daysLeft = 0;
                try {
                    daysLeft = ChronoUnit.DAYS.between(today, LocalDate.parse(deadline));
                } catch (Exception ignored) {}
                list.add(new ReminderEntry(
                    rs.getString("title"),
                    rs.getString("course_name") != null ? rs.getString("course_name") : "—",
                    deadline,
                    rs.getString("priority"),
                    daysLeft
                ));
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("ReminderService.fetchUpcoming: " + e.getMessage());
        }
        return list;
    }

    // ── Styled dialog ─────────────────────────────────────────────────────

    private static void showReminderDialog(List<ReminderEntry> entries, Stage ownerStage) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("📅  Deadline Reminders");
        dialog.setHeaderText(null);
        if (ownerStage != null) dialog.initOwner(ownerStage);

        // ── Root layout ──────────────────────────────────────────────────
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #f5f0ff;");
        root.setPrefWidth(460);

        // ── Header ───────────────────────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 14, 20));
        header.setStyle("-fx-background-color: #3b0764; -fx-background-radius: 0;");

        Label bell = new Label("🔔");
        bell.setStyle("-fx-font-size: 22px;");

        VBox headerText = new VBox(2);
        Label title = new Label("Upcoming Deadlines");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label sub = new Label(entries.size() + " task" + (entries.size() > 1 ? "s" : "") +
                              " due within the next 7 days");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #e9d5ff;");
        headerText.getChildren().addAll(title, sub);
        header.getChildren().addAll(bell, headerText);

        // ── Scrollable task list ─────────────────────────────────────────
        VBox taskList = new VBox(8);
        taskList.setPadding(new Insets(14, 16, 14, 16));
        taskList.setStyle("-fx-background-color: #f5f0ff;");

        for (ReminderEntry entry : entries) {
            taskList.getChildren().add(buildTaskRow(entry));
        }

        ScrollPane scroll = new ScrollPane(taskList);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(340);
        scroll.setStyle("-fx-background: #f5f0ff; -fx-background-color: #f5f0ff;" +
                        "-fx-border-color: transparent;");

        // ── Footer note ──────────────────────────────────────────────────
        Label footer = new Label("💡  Tip: Mark tasks as complete in My Tasks to clear them.");
        footer.setStyle("-fx-font-size: 10px; -fx-text-fill: #9e9e9e; -fx-padding: 0 16 12 16;");
        footer.setWrapText(true);

        root.getChildren().addAll(header, scroll, footer);
        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().setStyle("-fx-background-color: #f5f0ff; -fx-padding: 0;");

        // ── Button ───────────────────────────────────────────────────────
        ButtonType okBtn = new ButtonType("Got it! ✓", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okBtn);
        Button ok = (Button) dialog.getDialogPane().lookupButton(okBtn);
        ok.setStyle(
            "-fx-background-color: #a855f7; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-padding: 10 28; -fx-background-radius: 8; -fx-cursor: hand;");

        dialog.showAndWait();
    }

    // ── Individual task row ───────────────────────────────────────────────

    private static HBox buildTaskRow(ReminderEntry entry) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Colour scheme based on urgency
        String accent, bg, badgeText;
        if (entry.daysLeft == 0) {
            accent    = "#b91c1c";
            bg        = "#fff1f2";
            badgeText = "TODAY!";
        } else if (entry.daysLeft == 1) {
            accent    = "#ea580c";
            bg        = "#fff7ed";
            badgeText = "TOMORROW";
        } else if (entry.daysLeft <= 3) {
            accent    = "#b45309";
            bg        = "#fffbeb";
            badgeText = entry.daysLeft + " DAYS";
        } else {
            accent    = "#1d4ed8";
            bg        = "#eff6ff";
            badgeText = entry.daysLeft + " DAYS";
        }

        row.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 10 12;" +
            "-fx-border-color: " + accent + ";" +
            "-fx-border-radius: 10;" +
            "-fx-border-width: 0 0 0 4;");   // left accent bar

        // Task info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label taskTitle = new Label(entry.title);
        taskTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;" +
                           "-fx-text-fill: #1e1e1e;");
        taskTitle.setWrapText(true);
        taskTitle.setMaxWidth(250);

        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label courseLbl = new Label("📚 " + entry.course);
        courseLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");

        Label dateLbl = new Label("📅 " + entry.deadline);
        dateLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280;");

        // Priority badge
        String priColor = switch (entry.priority) {
            case "High"   -> "#fee2e2;-fx-text-fill:#b91c1c";
            case "Medium" -> "#fef3c7;-fx-text-fill:#b45309";
            default       -> "#dcfce7;-fx-text-fill:#15803d";
        };
        Label priLbl = new Label(entry.priority);
        priLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: bold;" +
                        "-fx-background-color: " + priColor + ";" +
                        "-fx-background-radius: 20; -fx-padding: 2 8;");

        meta.getChildren().addAll(courseLbl, dateLbl, priLbl);
        info.getChildren().addAll(taskTitle, meta);

        // Urgency badge (right side)
        Label urgency = new Label(badgeText);
        urgency.setStyle(
            "-fx-background-color: " + accent + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 10px; -fx-font-weight: bold;" +
            "-fx-background-radius: 6; -fx-padding: 4 10;");
        urgency.setMinWidth(75);
        urgency.setAlignment(Pos.CENTER);

        row.getChildren().addAll(info, urgency);
        return row;
    }
}