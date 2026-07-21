package com.praneeth.urlservice.service;

import com.praneeth.urlservice.dto.CreateShortUrlRequest;
import com.praneeth.urlservice.dto.ShortUrlResponse;
import com.praneeth.urlservice.dto.UpdateShortUrlRequest;
import com.praneeth.urlservice.entity.ShortUrl;
import com.praneeth.urlservice.exception.ShortCodeAlreadyExistsException;
import com.praneeth.urlservice.exception.ShortUrlNotFoundException;
import com.praneeth.urlservice.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

@Service
public class ShortUrlService {
    private static final String CODE_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String REDIRECT_CACHE_PREFIX = "shortlink:redirect:";

    private final ShortUrlRepository shortUrlRepository;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String baseUrl;
    private final int codeLength;
    private final Duration redirectCacheTtl;

    public ShortUrlService(
            ShortUrlRepository shortUrlRepository,
            StringRedisTemplate redisTemplate,
            @Value("${application.short-url.base-url}")
            String baseUrl,
            @Value("${application.short-url.code-length:7}")
            int codeLength,
            @Value("${application.short-url.redirect-cache-ttl-seconds:300}")
            long redirectCacheTtlSeconds
    ) {
        this.shortUrlRepository = shortUrlRepository;
        this.redisTemplate = redisTemplate;
        this.baseUrl = baseUrl;
        this.codeLength = codeLength;
        this.redirectCacheTtl = Duration.ofSeconds(redirectCacheTtlSeconds);
    }

    @Transactional
    public ShortUrlResponse create(String userId, CreateShortUrlRequest request) {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setUserId(userId);
        shortUrl.setLongUrl(request.longUrl());
        shortUrl.setShortCode(resolveShortCode(request.customAlias()));
        shortUrl.setExpiresAt(request.expiresAt());

        return toResponse(shortUrlRepository.save(shortUrl));
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlResponse> findAll(String userId, Pageable pageable) {
        return shortUrlRepository.findAllByUserIdAndDeletedFalse(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse findById(String userId, String id) {
        return toResponse(getOwnedShortUrl(userId, id));
    }

    @Transactional
    public ShortUrlResponse update(String userId, String id, UpdateShortUrlRequest request) {
        ShortUrl shortUrl = getOwnedShortUrl(userId, id);
        String previousCode = shortUrl.getShortCode();

        if (request.longUrl() != null) {
            shortUrl.setLongUrl(request.longUrl());
        }

        if (request.customAlias() != null && !request.customAlias().equals(shortUrl.getShortCode())) {
            ensureShortCodeAvailable(request.customAlias());
            shortUrl.setShortCode(request.customAlias());
        }

        if (request.expiresAt() != null) {
            shortUrl.setExpiresAt(request.expiresAt());
        }

        ShortUrlResponse response = toResponse(shortUrlRepository.save(shortUrl));
        evictRedirectCache(previousCode);
        evictRedirectCache(response.shortCode());
        return response;
    }

    @Transactional
    public ShortUrlResponse setEnabled(String userId, String id, boolean enabled) {
        ShortUrl shortUrl = getOwnedShortUrl(userId, id);
        shortUrl.setEnabled(enabled);
        ShortUrlResponse response = toResponse(shortUrlRepository.save(shortUrl));
        evictRedirectCache(response.shortCode());
        return response;
    }

    @Transactional
    public void delete(String userId, String id) {
        ShortUrl shortUrl = getOwnedShortUrl(userId, id);
        shortUrl.setDeleted(true);
        shortUrlRepository.save(shortUrl);
        evictRedirectCache(shortUrl.getShortCode());
    }

    @Transactional(readOnly = true)
    public String resolveRedirectUrl(String shortCode) {
        String cachedUrl = readRedirectCache(shortCode);
        if (cachedUrl != null) {
            return cachedUrl;
        }

        ShortUrl shortUrl = shortUrlRepository.findByShortCodeAndDeletedFalse(shortCode)
                .orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));

        if (!shortUrl.isEnabled()) {
            throw new ShortUrlNotFoundException("Short URL is disabled");
        }

        if (shortUrl.getExpiresAt() != null && shortUrl.getExpiresAt().isBefore(Instant.now())) {
            throw new ShortUrlNotFoundException("Short URL has expired");
        }

        writeRedirectCache(shortCode, shortUrl.getLongUrl(), shortUrl.getExpiresAt());
        return shortUrl.getLongUrl();
    }

    private ShortUrl getOwnedShortUrl(String userId, String id) {
        return shortUrlRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ShortUrlNotFoundException("Short URL not found"));
    }

    private String resolveShortCode(String customAlias) {
        if (customAlias != null && !customAlias.isBlank()) {
            ensureShortCodeAvailable(customAlias);
            return customAlias;
        }

        String code;
        do {
            code = generateCode();
        } while (shortUrlRepository.existsByShortCode(code));

        return code;
    }

    private void ensureShortCodeAvailable(String shortCode) {
        if (shortUrlRepository.existsByShortCode(shortCode)) {
            throw new ShortCodeAlreadyExistsException("Short code is already in use");
        }
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            builder.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String readRedirectCache(String shortCode) {
        try {
            return redisTemplate.opsForValue().get(REDIRECT_CACHE_PREFIX + shortCode);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void writeRedirectCache(String shortCode, String longUrl, Instant expiresAt) {
        try {
            Duration ttl = redirectCacheTtl;
            if (expiresAt != null) {
                Duration untilExpiry = Duration.between(Instant.now(), expiresAt);
                if (untilExpiry.isNegative() || untilExpiry.isZero()) {
                    return;
                }
                ttl = untilExpiry.compareTo(redirectCacheTtl) < 0 ? untilExpiry : redirectCacheTtl;
            }
            redisTemplate.opsForValue().set(REDIRECT_CACHE_PREFIX + shortCode, longUrl, ttl);
        } catch (RuntimeException ignored) {
            // Redis is an optimization; redirects still work from MySQL if it is unavailable.
        }
    }

    private void evictRedirectCache(String shortCode) {
        try {
            redisTemplate.delete(REDIRECT_CACHE_PREFIX + shortCode);
        } catch (RuntimeException ignored) {
            // Cache eviction failure must not break URL management.
        }
    }

    private ShortUrlResponse toResponse(ShortUrl shortUrl) {
        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getLongUrl(),
                shortUrl.getShortCode(),
                baseUrl + "/" + shortUrl.getShortCode(),
                shortUrl.isEnabled(),
                shortUrl.getExpiresAt(),
                shortUrl.getCreatedAt(),
                shortUrl.getUpdatedAt()
        );
    }
}