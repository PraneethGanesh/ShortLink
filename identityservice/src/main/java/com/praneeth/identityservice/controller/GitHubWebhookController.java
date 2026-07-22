package com.praneeth.identityservice.controller;

import com.praneeth.identityservice.dto.GitHubWebhookResponse;
import com.praneeth.identityservice.service.GitHubWebhookService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubWebhookController {

    private final GitHubWebhookService webhookService;

    public GitHubWebhookController(
            GitHubWebhookService webhookService
    ) {
        this.webhookService = webhookService;
    }

    @PostMapping("/webhooks")
    public ResponseEntity<GitHubWebhookResponse> receiveWebhook(
            @RequestHeader("X-GitHub-Event")
            String event,

            @RequestHeader("X-GitHub-Delivery")
            String deliveryId,

            @RequestHeader("X-Hub-Signature-256")
            String signature,

            @RequestBody
            byte[] payload
    ) {
        GitHubWebhookResponse response =
                webhookService.process(
                        event,
                        deliveryId,
                        signature,
                        payload
                );

        return ResponseEntity.ok(response);
    }
}