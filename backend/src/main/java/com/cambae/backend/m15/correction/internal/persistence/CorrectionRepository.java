package com.cambae.backend.m15.correction.internal.persistence;

import com.cambae.backend.common.data.AppendOnlyRepository;
import com.cambae.backend.m15.correction.internal.domain.Correction;
import java.util.List;

public interface CorrectionRepository extends AppendOnlyRepository<Correction, Long> {

    List<Correction> findByRecordTypeAndOriginalRecordIdOrderBySequenceNo(String recordType, long originalRecordId);
}
