package controller;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import database.DatabaseManager;
import java.sql.*;

public class ChartController {

    @FXML private PieChart statusPieChart;
    @FXML private BarChart<String, Number> priorityBarChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private Button backButton;
    @FXML private Label totalLabel;

    private int currentStudentId = 1;

    public void setStudentId(int studentId) {
        this.currentStudentId = studentId;
        loadCharts();
    }

    @FXML
    public void initialize() {
        xAxis.setLabel("Priority");
        yAxis.setLabel("Number of Tasks");
    }

    private void loadCharts() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            // ── Pie Chart — tasks by status ──────────────────────────
            String statusSql = "SELECT status, COUNT(*) as count FROM task " +
                              "WHERE student_id = ? GROUP BY status";
            PreparedStatement statusStmt = conn.prepareStatement(statusSql);
            statusStmt.setInt(1, currentStudentId);
            ResultSet statusRs = statusStmt.executeQuery();

            javafx.collections.ObservableList<PieChart.Data> pieData =
                FXCollections.observableArrayList();

            int total = 0;
            while (statusRs.next()) {
                int count = statusRs.getInt("count");
                total += count;
                pieData.add(new PieChart.Data(
                    statusRs.getString("status") + " (" + count + ")", count));
            }

            statusPieChart.setData(pieData);
            statusPieChart.setTitle("Tasks by Status");
            totalLabel.setText("Total Tasks: " + total);

            // ── Bar Chart — tasks by priority ─────────────────────────
            String prioritySql = "SELECT priority, COUNT(*) as count FROM task " +
                                "WHERE student_id = ? GROUP BY priority";
            PreparedStatement priorityStmt = conn.prepareStatement(prioritySql);
            priorityStmt.setInt(1, currentStudentId);
            ResultSet priorityRs = priorityStmt.executeQuery();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Tasks");

            while (priorityRs.next()) {
                series.getData().add(new XYChart.Data<>(
                    priorityRs.getString("priority"),
                    priorityRs.getInt("count")));
            }

            priorityBarChart.getData().clear();
            priorityBarChart.getData().add(series);
            priorityBarChart.setTitle("Tasks by Priority");

        } catch (SQLException e) {
            System.out.println("Error loading charts: " + e.getMessage());
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
}