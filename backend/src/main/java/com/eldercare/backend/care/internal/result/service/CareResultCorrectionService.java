package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.common.Reason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Transaction HTTP dùng READ_COMMITTED để quy tắc sau đính chính đọc projection mới. */
@Service
public class CareResultCorrectionService {
    private final CorrectionApi corrections;

    public CareResultCorrectionService(CorrectionApi corrections) {
        this.corrections = corrections;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void correct(long id, CorrectionType type, Object content, Reason reason) {
        corrections.create("M04.CARE_RESULT", id, type, content, reason);
    }
}
