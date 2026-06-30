package com.muni.demo.repository;

import com.muni.demo.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for URL mappings.
 */
@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Find mapping by short code.
     */
    Optional<UrlMapping> findByShortCode(String shortCode);

    /**
     * Find mapping by custom alias.
     */
    Optional<UrlMapping> findByCustomAlias(String customAlias);

    /**
     * Check if a short code already exists.
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Check if a custom alias already exists.
     */
    boolean existsByCustomAlias(String customAlias);
}
