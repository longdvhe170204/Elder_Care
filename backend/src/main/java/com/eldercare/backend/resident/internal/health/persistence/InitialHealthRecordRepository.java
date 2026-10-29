package com.eldercare.backend.resident.internal.health.persistence;

import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InitialHealthRecordRepository extends JpaRepository<InitialHealthRecord, Long> {

    Optional<InitialHealthRecord> findByResidentId(long residentId);
}
