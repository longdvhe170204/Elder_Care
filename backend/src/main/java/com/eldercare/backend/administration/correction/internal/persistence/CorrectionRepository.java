package com.eldercare.backend.administration.correction.internal.persistence;

import com.eldercare.backend.administration.correction.internal.domain.Correction;
import com.eldercare.backend.common.data.AppendOnlyRepository;
import java.util.List;

public interface CorrectionRepository extends AppendOnlyRepository<Correction, Long> {

    List<Correction> findByRecordTypeAndOriginalRecordIdOrderBySequenceNo(String recordType, long originalRecordId);
}
