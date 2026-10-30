package model;

import java.time.LocalDateTime;

public class Comment {

    private Long id;
    private Long reportId;
    private Long userId;
    private String text;
    private String photoPath;
    private boolean internal;
    private LocalDateTime createdAt;

    // Konstruktor für neue Kommentare
    public Comment(Long reportId, Long userId, String text) {
        this(reportId, userId, text, "");
    }

    public Comment(Long reportId, Long userId, String text,
                   String photoPath) {
        this(reportId, userId, text, photoPath, false);
    }

    public Comment(Long reportId, Long userId, String text,
                   String photoPath, boolean internal) {
        this.reportId = reportId;
        this.userId = userId;
        this.text = text;
        this.photoPath = photoPath;
        this.internal = internal;
    }

    // Konstruktor für Kommentare aus der Datenbank
    public Comment(Long id, Long reportId, Long userId,
                   String text, LocalDateTime createdAt) {
        this(id, reportId, userId, text, "", createdAt);
    }

    public Comment(Long id, Long reportId, Long userId, String text,
                   String photoPath, LocalDateTime createdAt) {
        this(id, reportId, userId, text, photoPath, false, createdAt);
    }

    public Comment(Long id, Long reportId, Long userId, String text,
                   String photoPath, boolean internal,
                   LocalDateTime createdAt) {

        this.id = id;
        this.reportId = reportId;
        this.userId = userId;
        this.text = text;
        this.photoPath = photoPath;
        this.internal = internal;
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

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
