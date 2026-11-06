package com.eldercare.backend.admission.internal.registration.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Nội dung dòng khai báo ở {@code simple_record} (loại {@code M02.RESIDENCE_REGISTRATION}; data-model 018 mục 0.1; R-15):
 * các cột của {@code khai_bao_tam_tru} cũ; trạng thái ở cột {@code status} của dòng. {@code replacedRegistrationId} đặt lúc
 * lập (khai báo cũ bị thay), ba trường xóa đặt một lần khi Đã xóa (FR-008).
 */
public record RegistrationContent(
        RegistrationType type,
        boolean renewal,
        Long obligationId,
        Instant dueAt,
        boolean localPermanentResident,
        String basisNote,
        LocalDate expiryDate,
        Long currentSubmissionId,
        Long evidenceFileId,
        Long replacedRegistrationId,
        Instant deregisteredAt,
        Long deregisteredById,
        Long deregistrationEvidenceFileId) {

    public static final SharedKind<RegistrationContent> KIND =
            SharedKind.of("M02.RESIDENCE_REGISTRATION", RegistrationContent.class);
}
