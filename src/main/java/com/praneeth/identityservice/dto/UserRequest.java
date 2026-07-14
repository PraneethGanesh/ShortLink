package com.praneeth.identityservice.dto;

public class UserRequest {
    private String name;
    private String email;
    private String passwordHash;
    private String confirmPassword;
    private String role;

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public String getRole() {
        return role;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
