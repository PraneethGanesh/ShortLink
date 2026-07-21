package com.praneeth.identityservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubWebhookController {

    @PostMapping("/webhooks")
    public ResponseEntity<Void> receiveWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader(value = "X-GitHub-Delivery", required = false)
            String deliveryId,
            @RequestBody String payload
    ) {
        System.out.println("GitHub event: " + event);
        System.out.println("Delivery ID: " + deliveryId);
        System.out.println(payload);

        return ResponseEntity.ok().build();
    }
}
