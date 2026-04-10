package model;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private String studentId;
    private String name;
    private String email;
    private String password;
    private List<Task> tasks;

    public Student(String studentId, String name, String email, String password) {
        this.studentId = studentId;
        this.name      = name;
        this.email     = email;
        this.password  = password;
        this.tasks     = new ArrayList<>();
    }

    // Getters
    public String getStudentId() { return studentId; }
    public String getName()      { return name; }
    public String getEmail()     { return email; }
    public String getPassword()  { return password; }
    public List<Task> getTasks() { return tasks; }

    // Setters
    public void setName(String name)         { this.name = name; }
    public void setEmail(String email)       { this.email = email; }
    public void setPassword(String password) { this.password = password; }

    // Add / remove tasks from in-memory list
    public void addTask(Task task) {
        tasks.add(task);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public List<Task> viewTasks() {
        return tasks;
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