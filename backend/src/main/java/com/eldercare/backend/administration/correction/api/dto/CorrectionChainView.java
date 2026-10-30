package com.eldercare.backend.administration.correction.api.dto;

import java.util.List;

/** Chuỗi đính chính của một bản gốc theo thứ tự thời gian, kèm nội dung gốc và hiện hành (FR-028). */
public record CorrectionChainView(
        String recordType,
        long originalRecordId,
        boolean corrected,
        boolean voided,
        Object originalContent,
        Object currentContent,
        List<CorrectionView> corrections) {}
