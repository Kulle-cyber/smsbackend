// src/main/java/com/example/model/User.java
package com.example.model;

public class User {
    private int id;
    private String username;
    private String passwordHash;
    private int roleId;
    private String fullName;
    private String email;

    public User() {}

    public User(int id, String username, String passwordHash, int roleId, String fullName, String email) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.roleId = roleId;
        this.fullName = fullName;
        this.email = email;
    }

    // getters/setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
