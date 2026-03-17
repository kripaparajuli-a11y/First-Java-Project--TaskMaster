package model;

public class TaskFactory {

    // Factory method - creates different types of tasks
    public static Task createTask(String type, String taskId, String title, 
                                  String description, String deadline, 
                                  String priority, String courseId, 
                                  String studentId) {
        switch (type.toLowerCase()) {
            case "assignment":
                return new Task(taskId, title, description, deadline, 
                               priority, "Not Started", courseId, 
                               studentId, "Assignment");
            case "exam":
                return new Task(taskId, title, description, deadline, 
                               priority, "Not Started", courseId, 
                               studentId, "Exam");
            case "project":
                return new Task(taskId, title, description, deadline, 
                               priority, "Not Started", courseId, 
                               studentId, "Project");
            case "quiz":
                return new Task(taskId, title, description, deadline, 
                               priority, "Not Started", courseId, 
                               studentId, "Quiz");
            case "presentation":
                return new Task(taskId, title, description, deadline, 
                               priority, "Not Started", courseId, 
                               studentId, "Presentation");
            default:
                return new Task(taskId, title, description, deadline, 
                               priority, "Not Started", courseId, 
                               studentId, "General");
        }
    }
}