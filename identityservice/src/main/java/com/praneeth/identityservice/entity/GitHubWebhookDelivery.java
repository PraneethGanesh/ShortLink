package com.praneeth.identityservice.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "github_webhook_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_github_delivery_id",
                columnNames = "delivery_id"
        )
)
public class GitHubWebhookDelivery {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "delivery_id", nullable = false, unique = true)
    private String deliveryId;

    @Column(nullable = false)
    private String event;

    @Column(nullable = false)
    private String action;

    private Long installationId;
    private String repositoryFullName;
    private Long pullRequestNumber;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant receivedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(Long installationId) {
        this.installationId = installationId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(Long pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }
}