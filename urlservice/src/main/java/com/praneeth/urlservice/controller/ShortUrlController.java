package com.praneeth.urlservice.controller;

import com.praneeth.urlservice.dto.CreateShortUrlRequest;
import com.praneeth.urlservice.dto.ShortUrlResponse;
import com.praneeth.urlservice.dto.UpdateShortUrlRequest;
import com.praneeth.urlservice.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {
    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateShortUrlRequest request
    ) {
        ShortUrlResponse response = shortUrlService.create(
                userId,
                request
        );

        return ResponseEntity.created(URI.create("/api/v1/urls/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ShortUrlResponse>> findAll(
            @RequestHeader("X-User-Id") String userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                shortUrlService.findAll(userId, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> findById(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id
    ) {
        return ResponseEntity.ok(
                shortUrlService.findById(userId, id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> update(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id,
            @Valid @RequestBody UpdateShortUrlRequest request
    ) {
        return ResponseEntity.ok(
                shortUrlService.update(userId, id, request)
        );
    }

    @PatchMapping("/{id}/enabled")
    public ResponseEntity<ShortUrlResponse> setEnabled(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id,
            @RequestParam boolean enabled
    ) {
        return ResponseEntity.ok(
                shortUrlService.setEnabled(userId, id, enabled)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String id
    ) {
        shortUrlService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}