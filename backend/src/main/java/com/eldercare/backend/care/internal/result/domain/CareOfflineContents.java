package com.eldercare.backend.care.internal.result.domain;

import com.eldercare.backend.common.Reason;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * 005 T037 (C-14; payload chủ dự án duyệt 2026-11-03, plan mục "Quyết định chủ dự án sau review sau merge" điểm 3): nội
 * dung hai loại bản ghi ngoại tuyến gửi qua {@code POST /sync}. {@code M04.CARE_TASK_RESULT}: {@code {taskId, action:
 * RECORD | NOT_PERFORM, recording?, reasonTypeId?, note?}} với {@code recording} là {@code CareResultWrite};
 * {@code M04.AD_HOC_RECORDING}: đúng thân {@code /ad-hoc-care-recordings}.
 */
public final class CareOfflineContents {
    private CareOfflineContents() {}

    /** Thân {@code CareResultWrite} trong bản ghi ngoại tuyến. */
    public record Recording(
            Instant performedAt,
            BigDecimal numericValue,
            String codeValue,
            Map<String, Object> jsonValue,
            Boolean upperLimitConfirmed,
            String note,
            Reason reason) {
        public CareResultInput input() {
            return new CareResultInput(
                    performedAt,
                    numericValue,
                    codeValue,
                    jsonValue,
                    Boolean.TRUE.equals(upperLimitConfirmed),
                    note,
                    reason);
        }
    }

    public record TaskResult(Long taskId, String action, Recording recording, Long reasonTypeId, String note) {}

    public record AdHocRecording(
            Long residentId,
            Long taskTypeId,
            Instant performedAt,
            BigDecimal numericValue,
            String codeValue,
            Map<String, Object> jsonValue,
            Boolean upperLimitConfirmed,
            String note,
            Reason reason,
            Boolean sameTypeTasksReviewed,
            Long originalTaskId) {
        public CareResultInput input() {
            return new CareResultInput(
                    performedAt,
                    numericValue,
                    codeValue,
                    jsonValue,
                    Boolean.TRUE.equals(upperLimitConfirmed),
                    note,
                    reason);
        }
    }
}
