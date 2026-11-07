package com.eldercare.backend.admission.internal.registration.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Nội dung nghĩa vụ khai báo ở {@code simple_record} (loại {@code M02.REGISTRATION_OBLIGATION}; data-model 018 mục 0.1; R-15):
 * các cột của {@code nghia_vu_khai_bao} cũ; Đang mở hay Đã hủy ở cột {@code status}. Dùng {@code simple_record} thay vì
 * {@code resident_module_state} vì dòng M02 của bảng đó đã là trạng thái lưu trú của 004 (một dòng cho mỗi người, module).
 */
public record ObligationContent(
        ObligationTrigger trigger,
        Instant triggerAt,
        Instant dueAt,
        long sourceId,
        Long registrationId,
        boolean submitted,
        Instant managerNotifiedAt,
        Instant cancelledAt) {

    public static final SharedKind<ObligationContent> KIND =
            SharedKind.of("M02.REGISTRATION_OBLIGATION", ObligationContent.class);
}
