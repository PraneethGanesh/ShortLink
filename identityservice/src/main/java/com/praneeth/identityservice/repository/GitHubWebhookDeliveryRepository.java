package com.praneeth.identityservice.repository;

import com.praneeth.identityservice.entity.GitHubWebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface GitHubWebhookDeliveryRepository
        extends JpaRepository<GitHubWebhookDelivery, UUID> {

    boolean existsByDeliveryId(String deliveryId);
}