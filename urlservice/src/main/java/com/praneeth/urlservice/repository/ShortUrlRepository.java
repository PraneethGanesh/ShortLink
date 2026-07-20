package com.praneeth.urlservice.repository;

import com.praneeth.urlservice.entity.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShortUrlRepository
        extends JpaRepository<ShortUrl, String> {

    boolean existsByShortCode(String shortCode);

    Optional<ShortUrl> findByShortCodeAndDeletedFalse(
            String shortCode
    );

    Optional<ShortUrl> findByIdAndUserIdAndDeletedFalse(
            String id,
            String userId
    );

    Page<ShortUrl> findAllByUserIdAndDeletedFalse(
            String userId,
            Pageable pageable
    );
}