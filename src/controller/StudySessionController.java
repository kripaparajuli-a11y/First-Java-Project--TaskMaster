package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.application.Platform;
import database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudySessionController {

    private static Timeline backgroundTimer  = null;
    private static boolean   timerRunning    = false;
    private static int       totalSeconds    = 0;
    private static int       elapsedSeconds  = 0;
    private static int       countdownStart  = 0;
    private static String    currentMode     = "STOPWATCH";

    private static volatile StudySessionController activeInstance = null;

    @FXML private Label studentNameLabel;
    @FXML private Label studentEmailLabel;
    @FXML private Label avatarInitial;          // ← ADDED
    @FXML private Label welcomeLabel;
    @FXML private Label totalStudyLabel;
    @FXML private Label timerLabel;
    @FXML private Label timerSubLabel;
    @FXML private Label modeLabel;
    @FXML private Button startStopButton;
    @FXML private Button stopwatchTab;
    @FXML private Button countdownTab;
    @FXML private HBox   countdownInputBox;
    @FXML private TextField countdownMinField;
    @FXML private TextField countdownSecField;
    @FXML private TextField notesField;
    @FXML private Label saveMessageLabel;
    @FXML private VBox  saveSessionBox;
    @FXML private TableView<String[]> historyTable;
    @FXML private TableColumn<String[], String> hDateColumn;
    @FXML private TableColumn<String[], String> hTypeColumn;
    @FXML private TableColumn<String[], String> hDurationColumn;
    @FXML private TableColumn<String[], String> hNotesColumn;

    private int    currentStudentId    = 1;
    private String currentStudentName  = "Student";
    private String currentStudentEmail = "";

    private String pendingSaveType    = null;
    private int    pendingSaveMinutes = 0;

    private final ObservableList<String[]> historyList =
        FXCollections.observableArrayList();

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void setStudentInfo(String name, String email, int studentId) {
        this.currentStudentName  = name;
        this.currentStudentEmail = email;
        this.currentStudentId    = studentId;
        updateSidebarProfile();
        loadHistory();
        updateTotalToday();
        syncUiToState();
    }

    private void updateSidebarProfile() {
        if (studentNameLabel  != null) studentNameLabel.setText(currentStudentName);
        if (studentEmailLabel != null) studentEmailLabel.setText(currentStudentEmail);
        if (welcomeLabel      != null) welcomeLabel.setText(currentStudentName + "!");
        if (avatarInitial     != null && !currentStudentName.isEmpty())   // ← ADDED
            avatarInitial.setText(String.valueOf(currentStudentName.charAt(0)).toUpperCase());
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        historyTable.setItems(historyList);
        Label ph = new Label("No sessions recorded yet.");
        ph.setStyle("-fx-text-fill:#a855f7;-fx-font-size:13px;-fx-font-style:italic;");
        historyTable.setPlaceholder(ph);

        activeInstance = this;

        if (currentMode.equals("COUNTDOWN")) {
            totalSeconds = 0; countdownStart = 0; elapsedSeconds = 0;
        }
    }

    // ── Timer ─────────────────────────────────────────────────────────────

    private static void startBackgroundTimer() {
        if (backgroundTimer != null) backgroundTimer.stop();
        timerRunning = true;
        backgroundTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        backgroundTimer.setCycleCount(Timeline.INDEFINITE);
        backgroundTimer.play();
    }

    private static void stopBackgroundTimer() {
        timerRunning = false;
        if (backgroundTimer != null) { backgroundTimer.stop(); backgroundTimer = null; }
    }

    private static void tick() {
        if (currentMode.equals("STOPWATCH")) {
            elapsedSeconds++;
            Platform.runLater(() -> {
                StudySessionController inst = activeInstance;
                if (inst != null) inst.syncUiToState();
            });
        } else {
            if (totalSeconds > 0) {
                totalSeconds--;
                elapsedSeconds++;
                Platform.runLater(() -> {
                    StudySessionController inst = activeInstance;
                    if (inst != null) inst.syncUiToState();
                });
                if (totalSeconds == 0) {
                    stopBackgroundTimer();
                    final int mins = elapsedSeconds / 60;
                    Platform.runLater(() -> {
                        StudySessionController inst = activeInstance;
                        if (inst != null) {
                            inst.pendingSaveType    = "Countdown";
                            inst.pendingSaveMinutes = mins;
                            inst.showSaveDialog();
                        }
                    });
                }
            }
        }
    }

    // ── Save dialog (Countdown auto-popup) ────────────────────────────────

    private void showSaveDialog() {
        if (pendingSaveType == null) { syncUiToState(); return; }

        String typeToSave  = pendingSaveType;
        int    elapsedMins = pendingSaveMinutes;
        int    minsToSave  = Math.max(1, elapsedMins);
        pendingSaveType = null; pendingSaveMinutes = 0;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Session Complete!");
        dialog.setHeaderText(null);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color:#f5f0ff;");

        String timeMsg = elapsedMins < 1
            ? "Countdown finished! (" + countdownStart + "s)"
            : "Countdown finished! (" + elapsedMins + " min)";

        Label lbl = new Label("⏳  " + timeMsg);
        lbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#3b0764;");
        Label sub = new Label("Save this session to your history?");
        sub.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;");
        TextField notesInput = new TextField();
        notesInput.setPromptText("Notes (optional)");
        notesInput.setStyle("-fx-background-color:white;-fx-text-fill:#3b0764;" +
            "-fx-prompt-text-fill:#aaa;-fx-padding:8 10;-fx-background-radius:8;" +
            "-fx-font-size:12px;-fx-border-color:#d8b4fe;-fx-border-radius:8;-fx-border-width:1;");
        content.getChildren().addAll(lbl, sub, notesInput);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setStyle("-fx-background-color:#f5f0ff;");

        ButtonType saveBtn = new ButtonType("💾  Save Session", ButtonBar.ButtonData.OK_DONE);
        ButtonType skipBtn = new ButtonType("Skip",             ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, skipBtn);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveBtn);
        saveButton.setStyle("-fx-background-color:#a855f7;-fx-text-fill:white;" +
            "-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:10 24;" +
            "-fx-background-radius:8;-fx-cursor:hand;");
        Button skipButton = (Button) dialog.getDialogPane().lookupButton(skipBtn);
        skipButton.setStyle("-fx-background-color:#AA336A;-fx-text-fill:white;" +
            "-fx-font-size:13px;-fx-font-weight:bold;-fx-padding:10 24;" +
            "-fx-background-radius:8;-fx-cursor:hand;");

        if (timerLabel != null && timerLabel.getScene() != null) {
            dialog.initOwner(timerLabel.getScene().getWindow());
        }

        final int finalMins = minsToSave;
        dialog.showAndWait().ifPresent(result -> {
            if (result == saveBtn) {
                String notes = notesInput.getText().trim();
                boolean ok = saveSessionToDb(typeToSave, finalMins, notes);
                if (ok) {
                    showSaveMsg("✅ Saved! (" + finalMins + " min)", "#15803d");
                    loadHistory();
                    updateTotalToday();
                } else {
                    showSaveMsg("❌ DB error — check console", "red");
                }
            }
        });
        syncUiToState();
    }

    // ── Sync UI ───────────────────────────────────────────────────────────

    private void syncUiToState() {
        String timeStr;
        if (currentMode.equals("STOPWATCH")) {
            timeStr = String.format("%02d:%02d", elapsedSeconds/60, elapsedSeconds%60);
        } else {
            timeStr = String.format("%02d:%02d", totalSeconds/60, totalSeconds%60);
        }
        if (timerLabel != null) timerLabel.setText(timeStr);

        if (currentMode.equals("STOPWATCH")) {
            if (modeLabel     != null) modeLabel.setText("⏱  Stopwatch");
            if (timerSubLabel != null) timerSubLabel.setText("Elapsed time");
            if (countdownInputBox != null) { countdownInputBox.setVisible(false); countdownInputBox.setManaged(false); }
            if (saveSessionBox   != null) { saveSessionBox.setVisible(true);  saveSessionBox.setManaged(true); }
            setActiveTab("stopwatch");
        } else {
            if (modeLabel     != null) modeLabel.setText("⏳  Countdown Timer");
            if (timerSubLabel != null) timerSubLabel.setText(
                totalSeconds <= 0 && elapsedSeconds > 0 ? "Done!" : "Time remaining");
            if (countdownInputBox != null) { countdownInputBox.setVisible(true); countdownInputBox.setManaged(true); }
            if (saveSessionBox   != null) { saveSessionBox.setVisible(false); saveSessionBox.setManaged(false); }
            setActiveTab("countdown");
        }

        if (startStopButton != null) {
            if (timerRunning) {
                startStopButton.setText("⏸  Pause");
                startStopButton.setStyle("-fx-background-color:#581c87;-fx-text-fill:white;" +
                    "-fx-font-size:14px;-fx-font-weight:bold;-fx-padding:12 28;-fx-background-radius:10;-fx-cursor:hand;");
            } else {
                startStopButton.setText("▶  Start");
                startStopButton.setStyle("-fx-background-color:#a855f7;-fx-text-fill:white;" +
                    "-fx-font-size:14px;-fx-font-weight:bold;-fx-padding:12 28;-fx-background-radius:10;-fx-cursor:hand;");
            }
        }
    }

    // ── Mode Switching ────────────────────────────────────────────────────

    @FXML public void switchStopwatch() {
        stopBackgroundTimer();
        currentMode = "STOPWATCH"; elapsedSeconds = 0;
        syncUiToState();
    }

    @FXML public void switchCountdown() {
        stopBackgroundTimer();
        currentMode = "COUNTDOWN";
        totalSeconds = 0; countdownStart = 0; elapsedSeconds = 0;
        if (countdownMinField != null) { countdownMinField.clear(); countdownMinField.setPromptText("00"); }
        if (countdownSecField != null) { countdownSecField.clear(); countdownSecField.setPromptText("00"); }
        syncUiToState();
    }

    @FXML public void handleSetCountdown() {
        if (timerRunning) return;
        try {
            int mins = countdownMinField.getText().isEmpty() ? 0 :
                Integer.parseInt(countdownMinField.getText().trim());
            int secs = countdownSecField.getText().isEmpty() ? 0 :
                Integer.parseInt(countdownSecField.getText().trim());
            if (mins < 0 || secs < 0 || secs > 59 || (mins == 0 && secs == 0)) {
                showSaveMsg("Enter a valid time!", "red"); return;
            }
            totalSeconds   = mins * 60 + secs;
            countdownStart = totalSeconds;
            elapsedSeconds = 0;
            countdownMinField.clear(); countdownMinField.setPromptText("00");
            countdownSecField.clear(); countdownSecField.setPromptText("00");
            syncUiToState();
            if (saveMessageLabel != null) saveMessageLabel.setText("");
            showSaveMsg("✅ Timer set to " + mins + "m " + secs + "s!", "#15803d");
        } catch (NumberFormatException e) { showSaveMsg("Enter numbers only!", "red"); }
    }

    @FXML public void handleStartStop() {
        if (!timerRunning && currentMode.equals("COUNTDOWN") && totalSeconds == 0) {
            showSaveMsg("Please set a time first!", "red"); return;
        }
        if (timerRunning) stopBackgroundTimer();
        else startBackgroundTimer();
        syncUiToState();
    }

    @FXML public void handleReset() {
        stopBackgroundTimer();
        elapsedSeconds = 0;
        if (currentMode.equals("COUNTDOWN")) {
            totalSeconds = countdownStart;
            if (countdownMinField != null) { countdownMinField.clear(); countdownMinField.setPromptText("00"); }
            if (countdownSecField != null) { countdownSecField.clear(); countdownSecField.setPromptText("00"); }
        }
        syncUiToState();
    }

    // ── Manual Save (Stopwatch) ───────────────────────────────────────────

    @FXML public void handleSaveSession() {
        if (elapsedSeconds < 60) {
            showSaveMsg("Run the timer for at least 1 minute first!", "red"); return;
        }
        int minutes = elapsedSeconds / 60;
        String notes = notesField != null ? notesField.getText().trim() : "";
        boolean ok = saveSessionToDb("Stopwatch", minutes, notes);
        if (ok) {
            showSaveMsg("✅ Session saved! (" + minutes + " min)", "#15803d");
            if (notesField != null) notesField.clear();
            loadHistory();
            updateTotalToday();
        } else {
            showSaveMsg("❌ DB error — check console", "red");
        }
    }

    // ── DB: Save ─────────────────────────────────────────────────────────

    private boolean saveSessionToDb(String type, int minutes, String notes) {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(true);
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO study_session " +
                "(student_id, task_id, task_title, start_time, end_time, duration_min, session_type, session_date, notes) " +
                "VALUES (?, NULL, NULL, NULL, NULL, ?, ?, CURDATE(), ?)");
            stmt.setInt(1, currentStudentId);
            stmt.setInt(2, minutes);
            stmt.setString(3, type);
            stmt.setString(4, notes.isEmpty() ? null : notes);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Save error: " + e.getMessage());
            return false;
        }
    }

    // ── DB: Load history ──────────────────────────────────────────────────

    private void loadHistory() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT session_id, session_date, session_type, duration_min, notes " +
                "FROM study_session WHERE student_id=? ORDER BY session_date DESC, session_id DESC");
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();

            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                int mins = rs.getInt("duration_min");
                String dur = mins >= 60 ? (mins/60)+"h "+(mins%60)+"m" : mins+" min";
                String n = rs.getString("notes");
                rows.add(new String[]{
                    rs.getString("session_date"),
                    rs.getString("session_type"),
                    dur,
                    n != null ? n : "—",
                    String.valueOf(rs.getInt("session_id"))
                });
            }
            rs.close(); stmt.close();

            Runnable update = () -> {
                historyList.clear();
                historyList.addAll(rows);
                hideScrollBars(historyTable);
            };
            if (Platform.isFxApplicationThread()) update.run();
            else Platform.runLater(update);

        } catch (SQLException e) {
            System.out.println("loadHistory error: " + e.getMessage());
        }
    }

    // ── DB: Today total ───────────────────────────────────────────────────

    private void updateTotalToday() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT COALESCE(SUM(duration_min),0) FROM study_session " +
                "WHERE student_id=? AND session_date=CURDATE()");
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int total = rs.getInt(1);
                String text = total >= 60
                    ? "Today: "+(total/60)+"h "+(total%60)+"m"
                    : "Total today: "+total+" min";
                if (Platform.isFxApplicationThread()) {
                    if (totalStudyLabel != null) totalStudyLabel.setText(text);
                } else {
                    Platform.runLater(() -> { if (totalStudyLabel != null) totalStudyLabel.setText(text); });
                }
            }
            rs.close(); stmt.close();
        } catch (SQLException e) { System.out.println("updateTotalToday error: " + e.getMessage()); }
    }

    // ── DB: Delete ────────────────────────────────────────────────────────

    @FXML public void handleDeleteSession() {
        String[] selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showSaveMsg("Select a session to delete!", "red"); return; }
        try {
            int id = Integer.parseInt(selected[4]);
            Connection conn = DatabaseManager.getInstance().getConnection();
            conn.setAutoCommit(true);
            PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM study_session WHERE session_id=?");
            stmt.setInt(1, id);
            stmt.executeUpdate();
            stmt.close();
            showSaveMsg("Deleted!", "#15803d");
            loadHistory();
            updateTotalToday();
        } catch (Exception e) { System.out.println("Delete error: " + e.getMessage()); }
    }

    // ── Hide scrollbars helper ────────────────────────────────────────────

    private void hideScrollBars(TableView<?> table) {
        table.lookupAll(".scroll-bar").forEach(node -> {
            node.setVisible(false);
            node.setManaged(false);
        });
    }

    // ── Table setup ───────────────────────────────────────────────────────

    private void setupTableColumns() {
        hDateColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        hTypeColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        hDurationColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        hNotesColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));

        hDateColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill:#3b0764;-fx-font-size:11px;-fx-font-weight:bold;-fx-alignment:CENTER;");
            }
        });
        hTypeColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); setStyle(""); return; }
                String bg = item.equals("Stopwatch") ? "#a78bfa" : "#67e8f9";
                Label b = new Label(item);
                b.setStyle("-fx-background-color:"+bg+";-fx-text-fill:white;-fx-font-size:10px;" +
                    "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10;");
                setGraphic(b); setAlignment(Pos.CENTER); setStyle("-fx-background-color:transparent;");
            }
        });
        hDurationColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); setStyle(""); return; }
                Label b = new Label(item);
                b.setStyle("-fx-background-color:#dcfce7;-fx-text-fill:#15803d;-fx-font-size:10px;" +
                    "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10;");
                setGraphic(b); setAlignment(Pos.CENTER); setStyle("-fx-background-color:transparent;");
            }
        });
        hNotesColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-text-fill:#6b7280;-fx-font-size:11px;-fx-font-style:italic;-fx-background-color:transparent;");
            }
        });
        historyTable.setRowFactory(tv -> new TableRow<String[]>() {
            @Override protected void updateItem(String[] row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setStyle("-fx-background-color: white;"); return; }
                String base = getIndex() % 2 == 0 ? "#faf5ff" : "#f3e8ff";
                setStyle("-fx-background-color:"+base+";");
                setOnMouseEntered(e -> setStyle("-fx-background-color:#e9d5ff;-fx-cursor:hand;"));
                setOnMouseExited(e -> setStyle("-fx-background-color:"+base+";"));
            }
        });

        historyTable.skinProperty().addListener((obs, o, n) -> {
            if (n != null) Platform.runLater(() -> { styleTableHeader(); hideScrollBars(historyTable); });
        });
        historyTable.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) Platform.runLater(() -> { styleTableHeader(); hideScrollBars(historyTable); });
        });
        historyTable.widthProperty().addListener((obs, o, n) ->
            Platform.runLater(() -> { styleTableHeader(); hideScrollBars(historyTable); }));
    }

    private void styleTableHeader() {
        historyTable.lookupAll(".column-header-background").forEach(n ->
            n.setStyle("-fx-background-color:#3b0764;"));
        historyTable.lookupAll(".column-header").forEach(n ->
            n.setStyle("-fx-background-color:#3b0764;-fx-border-color:transparent #581c87 transparent transparent;-fx-border-width:0 1 0 0;"));
        historyTable.lookupAll(".label").forEach(n -> {
            if (n.getParent() != null && n.getParent().getStyleClass().contains("column-header"))
                n.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:12px;");
        });
        historyTable.lookupAll(".filler").forEach(n ->
            n.setStyle("-fx-background-color:#3b0764;"));
        historyTable.lookupAll(".scroll-bar").forEach(n -> {
            n.setVisible(false); n.setManaged(false);
        });
    }

    private void setActiveTab(String tab) {
        String base = "-fx-font-size:12px;-fx-font-weight:bold;-fx-padding:10 0;-fx-cursor:hand;";
        if (stopwatchTab != null)
            stopwatchTab.setStyle(base + (tab.equals("stopwatch")
                ? "-fx-background-color:#a855f7;-fx-text-fill:white;-fx-background-radius:10 0 0 10;"
                : "-fx-background-color:transparent;-fx-text-fill:#581c87;-fx-background-radius:10 0 0 10;"));
        if (countdownTab != null)
            countdownTab.setStyle(base + (tab.equals("countdown")
                ? "-fx-background-color:#a855f7;-fx-text-fill:white;-fx-background-radius:0 10 10 0;"
                : "-fx-background-color:transparent;-fx-text-fill:#581c87;-fx-background-radius:0 10 10 0;"));
    }

    private void showSaveMsg(String msg, String color) {
        if (saveMessageLabel != null) {
            saveMessageLabel.setText(msg);
            saveMessageLabel.setStyle("-fx-text-fill:"+color+";-fx-font-size:11px;-fx-font-weight:bold;");
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @FXML public void goHome()         { goToDashboard("/view/home.fxml",      "TaskMaster - Home"); }
    @FXML public void openTasks()      { goToDashboard("/view/dashboard.fxml", "TaskMaster - My Tasks"); }
    @FXML public void openStudyTimer() { loadHistory(); updateTotalToday(); syncUiToState(); }

    @FXML public void openCalendar() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/calendar.fxml"));
            javafx.scene.Parent root = loader.load();
            CalendarController cal = loader.getController();
            cal.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - Calendar");
        } catch (Exception e) { System.out.println("openCalendar: " + e.getMessage()); }
    }

    @FXML public void openGpa() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/gpa.fxml"));
            javafx.scene.Parent root = loader.load();
            GpaController gpa = loader.getController();
            gpa.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - My Grades");
        } catch (Exception e) { System.out.println("openGpa: " + e.getMessage()); }
    }

    @FXML public void handleLogout() {
        stopBackgroundTimer();
        activeInstance = null;
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/view/login.fxml"));
            getStage().setScene(new javafx.scene.Scene(loader.load(), 1000, 620));
            getStage().setTitle("TaskMaster");
        } catch (Exception e) { System.out.println("logout: " + e.getMessage()); }
    }

    private void goToDashboard(String fxml, String title) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxml));
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
        } catch (Exception e) { System.out.println("nav: " + e.getMessage()); }
    }

    private javafx.stage.Stage getStage() {
        if (studentNameLabel != null) return (javafx.stage.Stage) studentNameLabel.getScene().getWindow();
        if (timerLabel       != null) return (javafx.stage.Stage) timerLabel.getScene().getWindow();
        return null;
    }
}