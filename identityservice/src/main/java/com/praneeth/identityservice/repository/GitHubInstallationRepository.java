package com.praneeth.identityservice.repository;

import com.praneeth.identityservice.entity.GitHubInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitHubInstallationRepository
        extends JpaRepository<GitHubInstallation, Long> {
}
