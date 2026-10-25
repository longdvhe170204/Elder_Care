package com.cambae.backend.m03.internal.history.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.*;
import com.cambae.backend.m03.internal.bed.persistence.ReadinessCorrectionStore;
import com.cambae.backend.m15.correction.api.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T048: kiểm quan hệ bed/history, chuẩn hóa ISO offset trước khi chuyển cơ chế đính chính dùng chung. */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class HistoryCorrections {
    private final CorrectionApi corrections;
    private final ReadinessCorrectionStore readiness;

    public HistoryCorrections(CorrectionApi corrections, ReadinessCorrectionStore readiness) {
        this.corrections = corrections;
        this.readiness = readiness;
    }

    public void allocation(long id, CorrectionType type, Map<String, Object> content, Reason reason) {
        Map<String, Object> normalized = content == null ? null : new LinkedHashMap<>(content);
        if (normalized != null)
            for (String key : List.of("startAt", "endAt"))
                if (normalized.get(key) instanceof String text) {
                    try {
                        normalized.put(
                                key,
                                Instant.parse(text)
                                        .truncatedTo(java.time.temporal.ChronoUnit.MICROS)
                                        .toString());
                    } catch (Exception error) {
                        throw new InvalidValueException("Mốc thời gian phải theo ISO-8601");
                    }
                }
        corrections.create("M03.BED_ALLOCATION", id, type, normalized, reason);
    }

    public void readiness(long bedId, long historyId, CorrectionType type, Map<String, Object> content, Reason reason) {
        var original = readiness.required(historyId);
        if (original.bedId() != bedId) throw new NotFoundException();
        corrections.create("M03.BED_READINESS_CONFIRMATION", historyId, type, content, reason);
    }
}
