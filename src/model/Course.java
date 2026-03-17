package model;

import java.util.ArrayList;
import java.util.List;

public class Course {
    // Attributes
    private String courseId;
    private String courseName;
    private int creditHours;
    private List<Task> tasks;

    // Constructor
    public Course(String courseId, String courseName, int creditHours) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.creditHours = creditHours;
        this.tasks = new ArrayList<>();
    }

    // Getters
    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public int getCreditHours() { return creditHours; }
    public List<Task> getTasks() { return tasks; }

    // Setters
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setCreditHours(int creditHours) { this.creditHours = creditHours; }

    // Methods
    public void addTask(Task task) {
        tasks.add(task);
        System.out.println("Task added to course: " + task.getTitle());
    }

    public void assignGrade(String taskId, double grade) {
        System.out.println("Grade " + grade + " assigned to task: " + taskId);
    }

    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", creditHours=" + creditHours +
                '}';
    }
}