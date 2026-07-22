package com.praneeth.identityservice.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.praneeth.identityservice.dto.GitHubWebhookResponse;
import com.praneeth.identityservice.entity.GitHubInstallation;
import com.praneeth.identityservice.entity.GitHubWebhookDelivery;
import com.praneeth.identityservice.exception.InvalidWebhookSignatureException;
import com.praneeth.identityservice.repository.GitHubInstallationRepository;
import com.praneeth.identityservice.repository.GitHubWebhookDeliveryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class GitHubWebhookService {

    private final GitHubWebhookDeliveryRepository deliveryRepository;
    private final GitHubInstallationRepository installationRepository;
    private final ObjectMapper objectMapper;
    private final byte[] webhookSecret;

    public GitHubWebhookService(
            GitHubWebhookDeliveryRepository deliveryRepository,
            GitHubInstallationRepository installationRepository,
            ObjectMapper objectMapper,
            @Value("${application.github.webhook-secret}")
            String webhookSecret
    ) {
        this.deliveryRepository = deliveryRepository;
        this.installationRepository = installationRepository;
        this.objectMapper = objectMapper;
        this.webhookSecret =
                webhookSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public GitHubWebhookResponse process(
            String event,
            String deliveryId,
            String signature,
            byte[] payload
    ) {
        /*
         * Always verify the signature before reading or processing
         * data from the webhook.
         */
        verifySignature(signature, payload);

        /*
         * GitHub may redeliver the same webhook. The delivery ID
         * allows us to avoid processing it more than once.
         */
        if (deliveryRepository.existsByDeliveryId(deliveryId)) {
            return new GitHubWebhookResponse(
                    deliveryId,
                    event,
                    "DUPLICATE",
                    "Delivery was already processed"
            );
        }

        try {
            JsonNode root = objectMapper.readTree(payload);

            String action = root.path("action").asText("none");

            Long installationId = getNullableLong(
                    root.path("installation").path("id")
            );

            String repositoryFullName = getNullableText(
                    root.path("repository").path("full_name")
            );

            Long pullRequestNumber = getNullableLong(
                    root.path("number")
            );

            /*
             * Save or update installation details when GitHub sends
             * an installation event.
             */
            if ("installation".equals(event) && installationId != null) {
                updateInstallation(
                        root,
                        action,
                        installationId
                );
            }

            boolean supportedEvent = isSupportedEvent(event);

            GitHubWebhookDelivery delivery =
                    new GitHubWebhookDelivery();

            delivery.setDeliveryId(deliveryId);
            delivery.setEvent(event);
            delivery.setAction(action);
            delivery.setInstallationId(installationId);
            delivery.setRepositoryFullName(repositoryFullName);
            delivery.setPullRequestNumber(pullRequestNumber);
            delivery.setStatus(
                    supportedEvent ? "PROCESSED" : "IGNORED"
            );
            delivery.setReceivedAt(Instant.now());

            deliveryRepository.save(delivery);

            if (!supportedEvent) {
                return new GitHubWebhookResponse(
                        deliveryId,
                        event,
                        "IGNORED",
                        "Event was received but is not currently handled"
                );
            }

            return new GitHubWebhookResponse(
                    deliveryId,
                    event,
                    "PROCESSED",
                    "Webhook processed successfully"
            );

        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid GitHub webhook payload",
                    exception
            );
        }
    }

    private void updateInstallation(
            JsonNode root,
            String action,
            Long installationId
    ) {
        JsonNode accountNode =
                root.path("installation").path("account");

        GitHubInstallation installation =
                installationRepository
                        .findById(installationId)
                        .orElseGet(GitHubInstallation::new);

        installation.setInstallationId(installationId);

        installation.setAccountLogin(
                getNullableText(accountNode.path("login"))
        );

        installation.setAccountType(
                getNullableText(accountNode.path("type"))
        );

        /*
         * Mark the installation inactive when it is deleted
         * or suspended.
         */
        boolean active =
                !"deleted".equalsIgnoreCase(action)
                        && !"suspend".equalsIgnoreCase(action);

        installation.setActive(active);
        installation.setUpdatedAt(Instant.now());

        installationRepository.save(installation);
    }

    private void verifySignature(
            String signature,
            byte[] payload
    ) {
        if (signature == null
                || !signature.startsWith("sha256=")) {

            throw new InvalidWebhookSignatureException(
                    "Missing or malformed GitHub signature"
            );
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret,
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] expectedSignature = mac.doFinal(payload);

            byte[] receivedSignature =
                    HexFormat.of().parseHex(
                            signature.substring("sha256=".length())
                    );

            /*
             * MessageDigest.isEqual performs a timing-safe comparison.
             */
            if (!MessageDigest.isEqual(
                    expectedSignature,
                    receivedSignature
            )) {
                throw new InvalidWebhookSignatureException(
                        "Invalid GitHub webhook signature"
                );
            }

        } catch (InvalidWebhookSignatureException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new InvalidWebhookSignatureException(
                    "Malformed GitHub webhook signature"
            );
        }
    }

    private boolean isSupportedEvent(String event) {
        return "ping".equals(event)
                || "installation".equals(event)
                || "installation_repositories".equals(event)
                || "pull_request".equals(event);
    }

    private String getNullableText(JsonNode node) {
        if (node == null
                || node.isMissingNode()
                || node.isNull()) {
            return null;
        }

        return node.asText();
    }

    private Long getNullableLong(JsonNode node) {
        if (node == null
                || node.isMissingNode()
                || node.isNull()) {
            return null;
        }

        return node.asLong();
    }
}