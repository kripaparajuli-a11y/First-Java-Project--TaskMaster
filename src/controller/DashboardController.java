package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Task;
import model.TaskFactory;

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

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, Student!");

        // Setup columns
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

        // Setup dropdowns
        priorityBox.setItems(FXCollections.observableArrayList("High", "Medium", "Low"));
        typeBox.setItems(FXCollections.observableArrayList(
            "Assignment", "Exam", "Project", "Quiz", "Presentation"));

        taskTable.setItems(taskList);
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

        Task task = TaskFactory.createTask(type, 
            "T" + (taskList.size() + 1), 
            title, "", deadline, priority, course, "S1");

        taskList.add(task);
        messageLabel.setText("Task added successfully!");
        messageLabel.setStyle("-fx-text-fill: green;");
        clearFields();
    }

    @FXML
    public void handleDeleteTask() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            taskList.remove(selected);
            messageLabel.setText("Task deleted!");
            messageLabel.setStyle("-fx-text-fill: green;");
        } else {
            messageLabel.setText("Please select a task to delete!");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleMarkComplete() {
        Task selected = taskTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.updateStatus("Completed");
            taskTable.refresh();
            messageLabel.setText("Task marked as completed!");
            messageLabel.setStyle("-fx-text-fill: green;");
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
}