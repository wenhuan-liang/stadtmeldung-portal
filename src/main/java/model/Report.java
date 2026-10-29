
package model;

import java.time.LocalDateTime;

import model.enums.Category;
import model.enums.Status;

public class Report {

    private Long id;
    private Long userId;
    private String title;
    private Category category;
    private String defectType;
    private String description;
    private String photoPath;
    private double latitude;
    private double longitude;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Konstruktor für neue Meldungen
    public Report(Long userId, Category category,
                  String description, double latitude,
                  double longitude) {

        this(userId, category == null ? "" : category.name(), category, "",
                description, "", latitude, longitude);
    }

    public Report(Long userId, String title, Category category,
                  String defectType, String description, String photoPath,
                  double latitude, double longitude) {
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.defectType = defectType;
        this.description = description;
        this.photoPath = photoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = Status.NEW;
    }

    // Konstruktor für Meldungen aus der Datenbank
    public Report(Long id, Long userId, Category category,
                  String description, double latitude,
                  double longitude, Status status,
                  LocalDateTime createdAt,
                  LocalDateTime updatedAt) {

        this(id, userId, category == null ? "" : category.name(),
                category, "", description,
                "", latitude, longitude, status, createdAt, updatedAt);
    }

    public Report(Long id, Long userId, String title, Category category,
                  String defectType, String description, String photoPath,
                  double latitude, double longitude, Status status,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.category = category;
        this.defectType = defectType;
        this.description = description;
        this.photoPath = photoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getter und Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getDefectType() {
        return defectType;
    }

    public void setDefectType(String defectType) {
        this.defectType = defectType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
