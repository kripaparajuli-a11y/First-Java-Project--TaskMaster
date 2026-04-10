package model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class StudySession {
    private String sessionId;
    private String taskId;
    private String startTime;
    private String endTime;
    private int duration; // in minutes

    public StudySession(String sessionId, String taskId, String startTime) {
        this.sessionId = sessionId;
        this.taskId    = taskId;
        this.startTime = startTime;
        this.endTime   = null;
        this.duration  = 0;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getTaskId()    { return taskId; }
    public String getStartTime() { return startTime; }
    public String getEndTime()   { return endTime; }
    public int getDuration()     { return duration; }

    // Setters
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setDuration(int duration)  { this.duration = duration; }

    // Calculate duration from startTime to endTime (format: "HH:mm")
    public int calculateDuration() {
        if (startTime == null || endTime == null) return 0;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
            LocalTime start = LocalTime.parse(startTime, fmt);
            LocalTime end   = LocalTime.parse(endTime, fmt);
            this.duration   = (int) ChronoUnit.MINUTES.between(start, end);
            return this.duration;
        } catch (Exception e) {
            System.out.println("Could not calculate duration: " + e.getMessage());
            return 0;
        }
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