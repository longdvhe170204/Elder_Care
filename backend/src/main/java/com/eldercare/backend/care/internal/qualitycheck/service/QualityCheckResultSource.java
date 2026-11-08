package com.eldercare.backend.care.internal.qualitycheck.service;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.care.api.spi.QualityCheckPort;
import com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds;
import com.eldercare.backend.common.error.NotFoundException;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 014 T024 ({@code KetQuaKiemTraPort}): 005 kiểm căn cứ công việc làm lại là mục Không đạt của đúng công việc gốc. */
@Component
public class QualityCheckResultSource implements QualityCheckPort {
    private final RecordEventApi records;

    public QualityCheckResultSource(RecordEventApi records) {
        this.records = records;
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
        if (item.taskId() != originalTaskId || !QualityCheckKinds.FAIL.equals(item.status())) return Optional.empty();
        return Optional.of(new FailedCheckView(qualityCheckResultId, item.shiftId()));
    }
}
