package model;

import java.util.ArrayList;
import java.util.List;

public class Student {
    // Attributes
    private String studentId;
    private String name;
    private String email;
    private String password;
    private List<Task> tasks;

    // Constructor
    public Student(String studentId, String name, String email, String password) {
        this.studentId = studentId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.tasks = new ArrayList<>();
    }

    // Getters
    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public List<Task> getTasks() { return tasks; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }

    // Methods
    public boolean login(String email, String password) {
        if (this.email.equals(email) && this.password.equals(password)) {
            System.out.println("Login successful: " + name);
            return true;
        }
        System.out.println("Login failed!");
        return false;
    }

    public void logout() {
        System.out.println("Student " + name + " logged out.");
    }

    public void addTask(Task task) {
        tasks.add(task);
        System.out.println("Task added: " + task.getTitle());
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        System.out.println("Task removed: " + task.getTitle());
    }

    public List<Task> viewTasks() {
        return tasks;
    }

    public double calculateGPA() {
        System.out.println("Calculating GPA for: " + name);
        return 0.0;
    }

    @Override
    public String toString() {
        return "Student{" +
                "studentId='" + studentId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}