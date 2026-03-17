package model;

public class StudySession {
    // Attributes
    private String sessionId;
    private String taskId;
    private String startTime;
    private String endTime;
    private int duration;

    // Constructor
    public StudySession(String sessionId, String taskId, String startTime) {
        this.sessionId = sessionId;
        this.taskId = taskId;
        this.startTime = startTime;
        this.endTime = null;
        this.duration = 0;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getTaskId() { return taskId; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public int getDuration() { return duration; }

    // Setters
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setDuration(int duration) { this.duration = duration; }

    // Methods
    public void startSession() {
        System.out.println("Study session started for task: " + taskId);
    }

    public void endSession(String endTime) {
        this.endTime = endTime;
        System.out.println("Study session ended for task: " + taskId);
    }

    public int calculateDuration() {
        System.out.println("Duration: " + duration + " minutes");
        return duration;
    }

    @Override
    public String toString() {
        return "StudySession{" +
                "sessionId='" + sessionId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", duration=" + duration +
                '}';
    }
}