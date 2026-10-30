package com.eldercare.backend.administration.correction.internal.persistence;

import com.eldercare.backend.administration.correction.internal.domain.Correction;
import com.eldercare.backend.common.data.AppendOnlyRepository;
import java.util.Collection;
import java.util.List;

public interface CorrectionRepository extends AppendOnlyRepository<Correction, Long> {

    /** Chuỗi của bản gốc theo loại hiện hành và các loại cũ ánh xạ về nó (research R-15 mục 5). */
    List<Correction> findByRecordTypeInAndOriginalRecordIdOrderBySequenceNo(
            Collection<String> recordTypes, long originalRecordId);
}
