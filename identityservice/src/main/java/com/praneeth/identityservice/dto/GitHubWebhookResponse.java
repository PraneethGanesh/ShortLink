package com.praneeth.identityservice.dto;

public record GitHubWebhookResponse(
        String deliveryId,
        String event,
        String status,
        String message
) {
}
