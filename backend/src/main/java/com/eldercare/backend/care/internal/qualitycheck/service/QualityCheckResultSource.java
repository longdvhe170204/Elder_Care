package com.eldercare.backend.care.internal.qualitycheck.service;

import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.care.api.spi.QualityCheckPort;
import com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds;
import com.eldercare.backend.common.error.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 014 T024 ({@code KetQuaKiemTraPort}): 005 kiểm căn cứ công việc làm lại là mục Không đạt của đúng công việc gốc. */
@Component
public class QualityCheckResultSource implements QualityCheckPort {
    private final RecordEventApi records;
    private final CorrectionApi corrections;

    public QualityCheckResultSource(RecordEventApi records, CorrectionApi corrections) {
        this.records = records;
        this.corrections = corrections;
    }

    @Override
    public Optional<FailedCheckView> failed(long qualityCheckResultId, long originalTaskId) {
        QualityCheckKinds.Item item;
        try {
            item = records.require(QualityCheckKinds.ITEMS, qualityCheckResultId)
                    .content();
        } catch (NotFoundException e) {
            return Optional.empty();
        }
        if (item.taskId() != originalTaskId) return Optional.empty();
        var current = corrections
                .current(QualityCheckKinds.ITEMS.code(), List.of(qualityCheckResultId))
                .get(qualityCheckResultId);
        String result = current != null && current.content() instanceof Map<?, ?> content
                ? (String) content.get("result")
                : item.status();
        if (!QualityCheckKinds.FAIL.equals(result)) return Optional.empty();
        return Optional.of(new FailedCheckView(qualityCheckResultId, item.shiftId()));
    }
}
