package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.application.Platform;
import database.DatabaseManager;
import java.sql.*;

public class GpaController {

    @FXML private Label studentNameLabel;
    @FXML private Label studentEmailLabel;
    @FXML private Label avatarInitial;
    @FXML private Label welcomeLabel;
    @FXML private Label overallLabel;

    @FXML private TextField courseNameField;
    @FXML private TextField semesterField;
    @FXML private Label courseMessageLabel;
    @FXML private ListView<String> courseListView;

    @FXML private Label selectedCourseLabel;
    @FXML private Label currentGradeLabel;
    @FXML private Label projectedGradeLabel;
    @FXML private Label weightWarningLabel;
    @FXML private Label formTitleLabel;
    @FXML private TextField assessmentNameField;
    @FXML private TextField weightField;
    @FXML private TextField scoreField;
    @FXML private Label assessmentMessageLabel;
    @FXML private Button addEditButton;

    @FXML private TableView<String[]> assessmentTable;
    @FXML private TableColumn<String[], String> aNameColumn;
    @FXML private TableColumn<String[], String> aWeightColumn;
    @FXML private TableColumn<String[], String> aScoreColumn;
    @FXML private TableColumn<String[], String> aGradeColumn;
    @FXML private TableColumn<String[], String> aContribColumn;

    private int currentStudentId = 1;
    private String currentStudentName = "Student";
    private String currentStudentEmail = "";

    private int selectedCourseGradeId = -1;
    private int editingAssessmentId   = -1;

    private ObservableList<String> courseDisplayList   = FXCollections.observableArrayList();
    private ObservableList<String[]> assessmentList    = FXCollections.observableArrayList();
    private java.util.Map<String, Integer> courseIdMap = new java.util.LinkedHashMap<>();
    private java.util.Map<Integer, Integer> rowToDbId  = new java.util.LinkedHashMap<>();

    public void setStudentInfo(String name, String email, int studentId) {
        this.currentStudentName  = name;
        this.currentStudentEmail = email;
        this.currentStudentId    = studentId;
        updateSidebarProfile();
        loadCourses();
        updateOverallSummary();
    }

    private void updateSidebarProfile() {
        if (studentNameLabel  != null) studentNameLabel.setText(currentStudentName);
        if (studentEmailLabel != null) studentEmailLabel.setText(currentStudentEmail);
        if (welcomeLabel      != null) welcomeLabel.setText(currentStudentName + "!");
        if (avatarInitial     != null && !currentStudentName.isEmpty())
            avatarInitial.setText(String.valueOf(currentStudentName.charAt(0)).toUpperCase());
    }

    @FXML
    public void initialize() {
        aNameColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[0]));
        aWeightColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[1]));
        aScoreColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[2]));
        aGradeColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[3]));
        aContribColumn.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(d.getValue()[4]));

        aNameColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("-fx-font-weight: bold; -fx-text-fill: #3b0764; -fx-font-size: 12px;");
            }
        });

        aWeightColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                Label b = new Label(item);
                b.setStyle("-fx-background-color: #ede9fe; -fx-text-fill: #6d28d9;" +
                    "-fx-font-size: 10px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 20; -fx-padding: 3 10;");
                setGraphic(b); setAlignment(Pos.CENTER);
            }
        });

        aScoreColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                double val = 0;
                try { val = Double.parseDouble(item.replace("%", "")); } catch (Exception ignored) {}
                String bg = val >= 80 ? "#dcfce7" : val >= 60 ? "#fef3c7" : "#fee2e2";
                String fg = val >= 80 ? "#15803d" : val >= 60 ? "#b45309" : "#b91c1c";
                Label b = new Label(item);
                b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                    "-fx-background-radius:20;-fx-padding:3 10;");
                setGraphic(b); setAlignment(Pos.CENTER);
            }
        });

        aGradeColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                String bg, fg;
                switch (item) {
                    case "A+" -> { bg = "#166534"; fg = "white"; }
                    case "A"  -> { bg = "#15803d"; fg = "white"; }
                    case "B+" -> { bg = "#1d4ed8"; fg = "white"; }
                    case "B"  -> { bg = "#2563eb"; fg = "white"; }
                    case "C+" -> { bg = "#b45309"; fg = "white"; }
                    case "C"  -> { bg = "#d97706"; fg = "white"; }
                    case "D"  -> { bg = "#ea580c"; fg = "white"; }
                    default   -> { bg = "#b91c1c"; fg = "white"; }
                }
                Label b = new Label(item);
                b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";" +
                    "-fx-font-size:11px;-fx-font-weight:bold;" +
                    "-fx-background-radius:20;-fx-padding:3 14;");
                setGraphic(b); setAlignment(Pos.CENTER);
            }
        });

        aContribColumn.setCellFactory(col -> new TableCell<String[], String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                if (empty || item == null) { setGraphic(null); return; }
                Label b = new Label(item);
                b.setStyle("-fx-background-color:#f3e8ff;-fx-text-fill:#7e22ce;" +
                    "-fx-font-size:10px;-fx-font-weight:bold;" +
                    "-fx-background-radius:20;-fx-padding:3 10;");
                setGraphic(b); setAlignment(Pos.CENTER);
            }
        });

        assessmentTable.setRowFactory(tv -> new TableRow<String[]>() {
            @Override protected void updateItem(String[] row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) { setStyle("-fx-background-color: white;"); return; }
                String base = "-fx-background-color: #faf5ff;";
                setStyle(base);
                setOnMouseEntered(e -> setStyle("-fx-background-color: #e9d5ff;"));
                setOnMouseExited(e -> setStyle(base));
            }
        });

        assessmentTable.skinProperty().addListener((obs, o, n) -> {
            if (n != null) Platform.runLater(() -> { styleAssessmentHeader(); hideScrollBars(assessmentTable); });
        });
        assessmentTable.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) Platform.runLater(() -> { styleAssessmentHeader(); hideScrollBars(assessmentTable); });
        });
        assessmentTable.widthProperty().addListener((obs, ov, nv) ->
            Platform.runLater(() -> { styleAssessmentHeader(); hideScrollBars(assessmentTable); }));

        assessmentTable.setItems(assessmentList);
        courseListView.setItems(courseDisplayList);

        courseListView.setCellFactory(lv -> new ListCell<String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(isSelected()
                    ? "-fx-background-color:#581c87;-fx-text-fill:white;-fx-font-size:12px;-fx-padding:8 12;"
                    : "-fx-background-color:transparent;-fx-text-fill:#3b0764;-fx-font-size:12px;-fx-padding:8 12;");
            }
        });
    }

    private void hideScrollBars(TableView<?> table) {
        table.lookupAll(".scroll-bar").forEach(node -> { node.setVisible(false); node.setManaged(false); });
    }

    private void styleAssessmentHeader() {
        assessmentTable.lookupAll(".column-header-background").forEach(n ->
            n.setStyle("-fx-background-color: #3b0764;"));
        assessmentTable.lookupAll(".column-header").forEach(n ->
            n.setStyle("-fx-background-color: #3b0764; -fx-border-color: #581c87; -fx-border-width: 0 1 0 0;"));
        assessmentTable.lookupAll(".column-header .label").forEach(n ->
            n.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px;"));
        assessmentTable.lookupAll(".filler").forEach(n ->
            n.setStyle("-fx-background-color: #3b0764;"));
    }

    private String letterGrade(double pct) {
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B+";
        if (pct >= 60) return "B";
        if (pct >= 50) return "C+";
        if (pct >= 40) return "C";
        if (pct >= 30) return "D";
        return "F";
    }

    private void loadCourses() {
        courseDisplayList.clear();
        courseIdMap.clear();
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, course_name, semester FROM course_grade WHERE student_id=? ORDER BY semester, course_name");
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String display = rs.getString("course_name") + "  [" + rs.getString("semester") + "]";
                courseDisplayList.add(display);
                courseIdMap.put(display, rs.getInt("id"));
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    @FXML public void handleAddCourse() {
        String name = courseNameField.getText().trim();
        String sem  = semesterField.getText().trim();
        if (name.isEmpty() || sem.isEmpty()) {
            showCourseMsg("Please fill in course name and semester!", "red"); return;
        }
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement check = conn.prepareStatement(
                "SELECT id FROM course_grade WHERE student_id=? AND course_name=? AND semester=?");
            check.setInt(1, currentStudentId); check.setString(2, name); check.setString(3, sem);
            if (check.executeQuery().next()) {
                showCourseMsg("This course already exists for that semester!", "red"); return;
            }
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO course_grade (student_id, course_name, semester) VALUES (?,?,?)");
            stmt.setInt(1, currentStudentId); stmt.setString(2, name); stmt.setString(3, sem);
            stmt.executeUpdate();
            showCourseMsg("Course added!", "#15803d");
            courseNameField.clear(); semesterField.clear();
            loadCourses(); updateOverallSummary();
        } catch (SQLException e) { showCourseMsg("Error: " + e.getMessage(), "red"); }
    }

    @FXML public void handleDeleteCourse() {
        String selected = courseListView.getSelectionModel().getSelectedItem();
        if (selected == null) { showCourseMsg("Select a course to delete!", "red"); return; }
        int id = courseIdMap.getOrDefault(selected, -1);
        if (id == -1) return;
        try {
            DatabaseManager.getInstance().getConnection()
                .prepareStatement("DELETE FROM course_grade WHERE id=" + id).executeUpdate();
            showCourseMsg("Course deleted!", "#15803d");
            selectedCourseGradeId = -1; editingAssessmentId = -1;
            selectedCourseLabel.setText("Select a course first");
            currentGradeLabel.setText(""); projectedGradeLabel.setText("");
            weightWarningLabel.setText(""); assessmentList.clear();
            loadCourses(); updateOverallSummary();
        } catch (SQLException e) { showCourseMsg("Error: " + e.getMessage(), "red"); }
    }

    @FXML public void handleCourseSelected() {
        String selected = courseListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        selectedCourseGradeId = courseIdMap.getOrDefault(selected, -1);
        selectedCourseLabel.setText("📝  " + selected);
        resetForm();
        loadAssessments();
    }

    private void loadAssessments() {
        assessmentList.clear();
        rowToDbId.clear();
        if (selectedCourseGradeId == -1) return;
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, name, weight, score FROM assessment WHERE course_grade_id=? ORDER BY id");
            stmt.setInt(1, selectedCourseGradeId);
            ResultSet rs = stmt.executeQuery();
            double totalWeight = 0, weightedScore = 0;
            int rowIdx = 0;
            while (rs.next()) {
                int dbId      = rs.getInt("id");
                double weight = rs.getDouble("weight");
                double score  = rs.getDouble("score");
                double contrib = Math.round(weight * score / 100.0 * 100.0) / 100.0;
                totalWeight   += weight;
                weightedScore += contrib;
                assessmentList.add(new String[]{
                    rs.getString("name"), weight + "%", score + "%",
                    letterGrade(score), contrib + "%", String.valueOf(dbId)
                });
                rowToDbId.put(rowIdx++, dbId);
            }
            updateCourseSummary(totalWeight, weightedScore);
            Platform.runLater(() -> { styleAssessmentHeader(); hideScrollBars(assessmentTable); });
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    private void updateCourseSummary(double totalWeight, double weightedScore) {
        currentGradeLabel.setText(String.format(
            "Earned so far: %.1f%% out of %.0f%% weight", weightedScore, totalWeight));
        if (totalWeight >= 99.9) {
            String letter = letterGrade(weightedScore);
            projectedGradeLabel.setText(String.format("Final Grade: %.1f%%  →  %s", weightedScore, letter));
            weightWarningLabel.setText("");
        } else {
            double proj = totalWeight > 0 ? weightedScore / totalWeight * 100 : 0;
            String letter = letterGrade(proj);
            projectedGradeLabel.setText(String.format("Projected if trend holds: %.1f%%  →  %s", proj, letter));
            weightWarningLabel.setText(String.format("⚠️  %.0f%% weight not yet added", 100 - totalWeight));
        }
    }

    @FXML public void handleEditAssessment() {
        String[] selected = assessmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAssessmentMsg("Select an assessment to edit!", "red"); return; }
        int rowIdx = assessmentTable.getSelectionModel().getSelectedIndex();
        editingAssessmentId = rowToDbId.getOrDefault(rowIdx, -1);
        assessmentNameField.setText(selected[0]);
        weightField.setText(selected[1].replace("%", ""));
        scoreField.setText(selected[2].replace("%", ""));
        addEditButton.setText("💾  Save Changes");
        formTitleLabel.setText("Editing: " + selected[0]);
        showAssessmentMsg("Edit the fields above then click Save Changes", "#a855f7");
    }

    @FXML public void handleAddOrEditAssessment() {
        if (editingAssessmentId != -1) saveEdit(); else addAssessment();
    }

    private void addAssessment() {
        if (selectedCourseGradeId == -1) { showAssessmentMsg("Please select a course first!", "red"); return; }
        String name = assessmentNameField.getText().trim();
        String wStr = weightField.getText().trim();
        String sStr = scoreField.getText().trim();
        if (name.isEmpty() || wStr.isEmpty() || sStr.isEmpty()) {
            showAssessmentMsg("Please fill in all fields!", "red"); return;
        }
        double weight, score;
        try { weight = Double.parseDouble(wStr); score = Double.parseDouble(sStr); }
        catch (NumberFormatException e) { showAssessmentMsg("Weight and Score must be numbers!", "red"); return; }
        if (weight <= 0 || weight > 100) { showAssessmentMsg("Weight must be 1–100!", "red"); return; }
        if (score < 0 || score > 100)    { showAssessmentMsg("Score must be 0–100!", "red"); return; }

        double currentTotal = assessmentList.stream()
            .mapToDouble(r -> Double.parseDouble(r[1].replace("%", ""))).sum();
        if (currentTotal + weight > 100.01) {
            showAssessmentMsg(String.format("Total weight would exceed 100%%! Currently at %.0f%%", currentTotal), "red"); return;
        }
        try {
            PreparedStatement stmt = DatabaseManager.getInstance().getConnection().prepareStatement(
                "INSERT INTO assessment (course_grade_id, name, weight, score) VALUES (?,?,?,?)");
            stmt.setInt(1, selectedCourseGradeId); stmt.setString(2, name);
            stmt.setDouble(3, weight); stmt.setDouble(4, score);
            stmt.executeUpdate();
            showAssessmentMsg("Assessment added!", "#15803d");
            clearAssessmentForm(); loadAssessments(); updateOverallSummary();
        } catch (SQLException e) { showAssessmentMsg("Error: " + e.getMessage(), "red"); }
    }

    private void saveEdit() {
        if (editingAssessmentId == -1) return;
        String name = assessmentNameField.getText().trim();
        String wStr = weightField.getText().trim();
        String sStr = scoreField.getText().trim();
        if (name.isEmpty() || wStr.isEmpty() || sStr.isEmpty()) {
            showAssessmentMsg("Please fill in all fields!", "red"); return;
        }
        double weight, score;
        try { weight = Double.parseDouble(wStr); score = Double.parseDouble(sStr); }
        catch (NumberFormatException e) { showAssessmentMsg("Weight and Score must be numbers!", "red"); return; }
        if (weight <= 0 || weight > 100) { showAssessmentMsg("Weight must be 1–100!", "red"); return; }
        if (score < 0 || score > 100)    { showAssessmentMsg("Score must be 0–100!", "red"); return; }

        final int editId = editingAssessmentId;
        double otherTotal = assessmentList.stream()
            .filter(r -> Integer.parseInt(r[5]) != editId)
            .mapToDouble(r -> Double.parseDouble(r[1].replace("%", ""))).sum();
        if (otherTotal + weight > 100.01) {
            showAssessmentMsg(String.format("Total weight would exceed 100%%! Others: %.0f%%", otherTotal), "red"); return;
        }
        try {
            PreparedStatement stmt = DatabaseManager.getInstance().getConnection().prepareStatement(
                "UPDATE assessment SET name=?, weight=?, score=? WHERE id=?");
            stmt.setString(1, name); stmt.setDouble(2, weight);
            stmt.setDouble(3, score); stmt.setInt(4, editingAssessmentId);
            stmt.executeUpdate();
            showAssessmentMsg("Updated!", "#15803d");
            resetForm(); loadAssessments(); updateOverallSummary();
        } catch (SQLException e) { showAssessmentMsg("Error: " + e.getMessage(), "red"); }
    }

    @FXML public void handleDeleteAssessment() {
        String[] selected = assessmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAssessmentMsg("Select an assessment to delete!", "red"); return; }
        int dbId = rowToDbId.getOrDefault(assessmentTable.getSelectionModel().getSelectedIndex(), -1);
        if (dbId == -1) return;
        try {
            DatabaseManager.getInstance().getConnection()
                .prepareStatement("DELETE FROM assessment WHERE id=" + dbId).executeUpdate();
            showAssessmentMsg("Deleted!", "#15803d");
            resetForm(); loadAssessments(); updateOverallSummary();
        } catch (SQLException e) { showAssessmentMsg("Error: " + e.getMessage(), "red"); }
    }

    private void resetForm() {
        editingAssessmentId = -1;
        clearAssessmentForm();
        addEditButton.setText("➕  Add Assessment");
        formTitleLabel.setText("Add Assessment (Assignment / Exam / Quiz...)");
        assessmentMessageLabel.setText("");
    }

    private void clearAssessmentForm() {
        assessmentNameField.clear();
        weightField.clear();
        scoreField.clear();
    }

    private void updateOverallSummary() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT id FROM course_grade WHERE student_id=?");
            stmt.setInt(1, currentStudentId);
            ResultSet rs = stmt.executeQuery();
            double totalPct = 0; int count = 0;
            while (rs.next()) {
                PreparedStatement a = conn.prepareStatement(
                    "SELECT weight, score FROM assessment WHERE course_grade_id=?");
                a.setInt(1, rs.getInt("id"));
                ResultSet ar = a.executeQuery();
                double tw = 0, ws = 0;
                while (ar.next()) {
                    double w = ar.getDouble("weight"), s = ar.getDouble("score");
                    tw += w; ws += w * s / 100.0;
                }
                if (tw >= 99.9) { totalPct += ws; count++; }
            }
            if (count > 0) {
                double avg = Math.round(totalPct / count * 10.0) / 10.0;
                String letter = letterGrade(avg);
                overallLabel.setText(String.format("Overall: %.1f%%  →  %s", avg, letter));
                overallLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;" +
                    "-fx-text-fill: #581c87; -fx-background-color: #f5f0ff;" +
                    "-fx-background-radius: 8; -fx-padding: 6 16;");
            } else {
                overallLabel.setText("No completed courses yet");
                overallLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #9e9e9e;" +
                    "-fx-background-color: #f5f0ff; -fx-background-radius: 8; -fx-padding: 6 16;");
            }
        } catch (SQLException e) { System.out.println("Error: " + e.getMessage()); }
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @FXML public void goHome()    { navigate("/view/home.fxml",      "TaskMaster - Home"); }
    @FXML public void openTasks() { navigate("/view/dashboard.fxml", "TaskMaster - My Tasks"); }
    @FXML public void openGpa()   { loadCourses(); updateOverallSummary(); }

    @FXML public void openCalendar() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/view/calendar.fxml"));
            javafx.scene.Parent root = loader.load();
            CalendarController cal = loader.getController();
            cal.setStudentInfo(currentStudentName, currentStudentEmail, currentStudentId);
            getStage().setScene(new javafx.scene.Scene(root, 1100, 680));
            getStage().setTitle("TaskMaster - Calendar");
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
        } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
    }

    private javafx.stage.Stage getStage() {
        if (studentNameLabel != null) return (javafx.stage.Stage) studentNameLabel.getScene().getWindow();
        if (overallLabel     != null) return (javafx.stage.Stage) overallLabel.getScene().getWindow();
        return null;
    }

    private void showCourseMsg(String msg, String color) {
        courseMessageLabel.setText(msg);
        courseMessageLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    private void showAssessmentMsg(String msg, String color) {
        assessmentMessageLabel.setText(msg);
        assessmentMessageLabel.setStyle("-fx-text-fill: " + color + ";");
    }
}