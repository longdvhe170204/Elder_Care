package com.cambae.backend.m01.internal.health.persistence;

import com.cambae.backend.m01.internal.health.domain.InitialHealthRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InitialHealthRecordRepository extends JpaRepository<InitialHealthRecord, Long> {

    Optional<InitialHealthRecord> findByResidentId(long residentId);
}
