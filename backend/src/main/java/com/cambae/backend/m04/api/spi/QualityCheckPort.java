package com.cambae.backend.m04.api.spi;

import java.util.Optional;

/** 014 cung cấp kết quả Không đạt kèm ca để kiểm căn cứ làm lại (FR-026a). */
public interface QualityCheckPort {
    Optional<FailedCheckView> failed(long qualityCheckResultId, long originalTaskId);

    record FailedCheckView(long id, long shiftId) {}
}
