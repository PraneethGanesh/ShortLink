package com.praneeth.identityservice.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String userId;
    @Column(nullable=false)
    private String name;
    @Column(nullable=false, unique=true)
    private String email;
    @Column(nullable=false)
    private String passwordHash;
    @Transient
    private String confirmPassword;
    @Enumerated(EnumType.STRING)
    private String role;

    private boolean enabled=true;
    private Instant createdAt=Instant.now();

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public void setRole(String role) {
        this.role = role;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
