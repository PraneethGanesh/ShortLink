package com.praneeth.identityservice.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubWebhookController {

    @PostMapping(
            value = "/webhooks",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestBody String payload
    ) {
        System.out.println("GitHub event received: " + event);
        return ResponseEntity.ok("Webhook received");
    }
}
