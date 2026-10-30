package model;

import java.time.LocalDateTime;

public class Comment {

    private Long id;
    private Long reportId;
    private Long userId;
    private String text;
    private LocalDateTime createdAt;

    // Konstruktor für neue Kommentare
    public Comment(Long reportId, Long userId, String text) {
        this.reportId = reportId;
        this.userId = userId;
        this.text = text;
    }

    // Konstruktor für Kommentare aus der Datenbank
    public Comment(Long id, Long reportId, Long userId,
                   String text, LocalDateTime createdAt) {

        this.id = id;
        this.reportId = reportId;
        this.userId = userId;
        this.text = text;
        this.createdAt = createdAt;
    }

    // Getter und Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}