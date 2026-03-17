package model;

public class Task {
    // Attributes
    private String taskId;
    private String title;
    private String description;
    private String deadline;
    private String priority;  // High, Medium, Low
    private String status;    // Not Started, In Progress, Completed, Overdue
    private String courseId;
    private String studentId;
    private String taskType;  // Assignment, Exam, Project, Quiz, Presentation

    // Constructor
    public Task(String taskId, String title, String description, 
                String deadline, String priority, String status, 
                String courseId, String studentId, String taskType) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.status = status;
        this.courseId = courseId;
        this.studentId = studentId;
        this.taskType = taskType;
    }

    // Getters
    public String getTaskId() { return taskId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDeadline() { return deadline; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public String getCourseId() { return courseId; }
    public String getStudentId() { return studentId; }
    public String getTaskType() { return taskType; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status) { this.status = status; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    // Methods
    public void createTask() {
        System.out.println("Task created: " + title);
    }

    public void editTask(String newTitle, String newDescription, 
                         String newDeadline, String newPriority) {
        this.title = newTitle;
        this.description = newDescription;
        this.deadline = newDeadline;
        this.priority = newPriority;
        System.out.println("Task updated: " + title);
    }

    public void deleteTask() {
        System.out.println("Task deleted: " + title);
    }

    public void updateStatus(String newStatus) {
        this.status = newStatus;
        System.out.println("Task status updated to: " + status);
    }

    @Override
    public String toString() {
        return "Task{" +
                "taskId='" + taskId + '\'' +
                ", title='" + title + '\'' +
                ", deadline='" + deadline + '\'' +
                ", priority='" + priority + '\'' +
                ", status='" + status + '\'' +
                ", taskType='" + taskType + '\'' +
                '}';
    }
}