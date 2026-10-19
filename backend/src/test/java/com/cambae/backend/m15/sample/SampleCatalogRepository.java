package com.cambae.backend.m15.sample;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleCatalogRepository extends JpaRepository<SampleCatalogItem, Long> {

    Optional<SampleCatalogItem> findByNameAndStatus(String name, com.cambae.backend.common.data.CatalogStatus status);
}
