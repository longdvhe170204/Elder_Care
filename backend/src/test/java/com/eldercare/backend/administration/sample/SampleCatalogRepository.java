package com.eldercare.backend.administration.sample;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleCatalogRepository extends JpaRepository<SampleCatalogItem, Long> {

    Optional<SampleCatalogItem> findByNameAndStatus(
            String name, com.eldercare.backend.common.data.CatalogStatus status);
}
