package com.eldercare.backend.care.internal.qualitycheck.service;

import static com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.*;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.time.BusinessTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T024 (FR-062): công việc đã được chọn mà kết quả bị Hủy ghi nhận thì mục chuyển Không kiểm tra được với lý do đó,
 * trong transaction của lệnh đính chính (005 {@code CareResultCorrectionProvider}). Không tính vào tỷ lệ Đạt.
 */
@Component
public class QualityCheckTaskVoids {
    static final String REASON = "Công việc đã hủy ghi nhận";

    private final RecordEventApi records;
    private final BusinessTime time;

    public QualityCheckTaskVoids(RecordEventApi records, BusinessTime time) {
        this.records = records;
        this.time = time;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void resultVoided(long taskId) {
        var row = records.findByUniqueKey(ITEMS, "QC_TASK:" + taskId);
        if (row.isEmpty() || row.get().confirmedAt() != null) return;
        var item = row.get().content();
        if (!PENDING.equals(item.status())) return;
        var now = time.now();
        records.updateDraft(
                ITEMS, row.get().id(), item.result(NOT_CHECKABLE, REASON, item.photoFileIds(), null, null, now));
        records.confirm(ITEMS, row.get().id(), now);
    }
}
