package com.eldercare.backend.admission.web.request;

import com.eldercare.backend.common.Reason;
import jakarta.validation.constraints.NotNull;

/** Ghi nhận đã liên hệ, kèm giường được giữ; lý do bắt buộc khi chọn không theo thứ tự (FR-014). */
public record RecordContactedBody(@NotNull Long bedId, Reason outOfOrderReason) {}
