package model;

import java.time.LocalDateTime;

public class Attachment {

    private Long id;
    private Long reportId;
    private String filePath;
    private LocalDateTime createdAt;

    // Konstruktor für neue Anhänge
    public Attachment(Long reportId, String filePath) {
        this.reportId = reportId;
        this.filePath = filePath;
    }

    // Konstruktor für Anhänge aus der Datenbank
    public Attachment(Long id, Long reportId,
                      String filePath, LocalDateTime createdAt) {

        this.id = id;
        this.reportId = reportId;
        this.filePath = filePath;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
