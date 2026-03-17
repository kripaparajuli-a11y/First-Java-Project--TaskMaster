package model;

public class Reminder {
    // Attributes
    private String reminderId;
    private String taskId;
    private String reminderDate;
    private String message;
    private boolean sent;

    // Constructor
    public Reminder(String reminderId, String taskId, 
                    String reminderDate, String message) {
        this.reminderId = reminderId;
        this.taskId = taskId;
        this.reminderDate = reminderDate;
        this.message = message;
        this.sent = false;
    }

    // Getters
    public String getReminderId() { return reminderId; }
    public String getTaskId() { return taskId; }
    public String getReminderDate() { return reminderDate; }
    public String getMessage() { return message; }
    public boolean isSent() { return sent; }

    // Setters
    public void setReminderDate(String reminderDate) { this.reminderDate = reminderDate; }
    public void setMessage(String message) { this.message = message; }

    // Methods
    public void sendReminder() {
        this.sent = true;
        System.out.println("Reminder sent: " + message);
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "reminderId='" + reminderId + '\'' +
                ", taskId='" + taskId + '\'' +
                ", reminderDate='" + reminderDate + '\'' +
                ", message='" + message + '\'' +
                ", sent=" + sent +
                '}';
    }
}
