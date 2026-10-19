package com.cambae.backend.m15.correction.internal;

import com.cambae.backend.common.data.AppendOnlyRepository;
import java.util.List;

interface CorrectionRepository extends AppendOnlyRepository<Correction, Long> {

    List<Correction> findByRecordTypeAndOriginalRecordIdOrderBySequenceNo(String recordType, long originalRecordId);
}
