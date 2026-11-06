package com.eldercare.backend.admission.internal.registration.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Nội dung một lần nộp ở {@code record_event} loại {@code M02.REGISTRATION_SUBMISSION} (data-model 018 mục 0.1; R-15): chỉ
 * thêm; kết quả đặt một lần từ {@code PENDING} khi dòng còn là bản nháp, rồi xác nhận. Cha của dòng là hồ sơ người cao tuổi
 * (để job loại bỏ theo hồ sơ không để lại dòng mồ côi khi khai báo bị xóa trước); khai báo của lần nộp ở {@code
 * registrationId}.
 */
public record SubmissionContent(
        long registrationId,
        LocalDate submittedDate,
        SubmissionChannel channel,
        String receivingAuthority,
        String dossierCode,
        SubmissionResult result,
        String rejectionReason,
        long recordedBy,
        Instant recordedAt) {

    public static final SharedKind<SubmissionContent> KIND =
            SharedKind.of("M02.REGISTRATION_SUBMISSION", SubmissionContent.class);
}
