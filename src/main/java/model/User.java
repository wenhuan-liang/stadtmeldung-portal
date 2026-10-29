package model;

import java.time.LocalDateTime;

import model.enums.Role;

public class User {

    private Long id;
    private String name;
    private String username;
    private String email;
    private String passwordHash;
    private Role role;
    private LocalDateTime createdAt;

    // Konstruktor für neue Benutzer
    public User(String name, String email,
                String passwordHash, Role role) {

        this.name = name;
        this.username = email;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Konstruktor für Benutzer aus der Datenbank
    public User(Long id, String name, String email,
                String passwordHash, Role role,
                LocalDateTime createdAt) {

        this.id = id;
        this.name = name;
        this.username = email;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    public User(Long id, String name, String username, String email,
                String passwordHash, Role role,
                LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
    }

    // Getter und Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
