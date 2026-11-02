package com.eldercare.backend.administration.correction.web.response;

import java.util.List;

public record ChainBody(
        String recordType,
        long originalRecordId,
        boolean corrected,
        boolean voided,
        Object originalContent,
        Object currentContent,
        List<CorrectionBody> corrections) {}
