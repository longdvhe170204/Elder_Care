package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.m02.internal.registration.domain.RegistrationSubmission;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.domain.SubmissionChannel;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationResidentStore;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationSubmissionStore;
import com.cambae.backend.m02.internal.registration.persistence.ResidenceRegistrationStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CorrectionView;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Đính chính khai báo {@code M02.RESIDENCE_REGISTRATION} ({@code DinhChinhDuocProvider}; research Z-04, FR-001, FR-003):
 * nội dung đã ghi (lần nộp hiện hành, kết quả, xác nhận thường trú cùng xã/phường) là nhóm 3, sửa sai qua chuỗi đính chính
 * của 000, dòng gốc giữ nguyên. Không gắn tầng (cách A người ghi gốc, hoặc C). Gỡ xác nhận thường trú cùng xã/phường nhầm
 * ({@code localPermanentResident: false}) đổi khai báo ngay, chỉ khi còn Cần khai báo; bằng chứng đúng được gắn vào khai
 * báo. Không Hủy ghi nhận cả khai báo (vòng đời theo bảng FR-005).
 */
@Component
class RegistrationCorrectionProvider implements CorrectableRecordProvider {

    private static final String LOCAL = "localPermanentResident";
    private static final Set<String> FIELDS = Set.of(
            "submittedDate",
            "channel",
            "receivingAuthority",
            "dossierCode",
            "rejectionReason",
            "expiryDate",
            "evidenceFileId",
            LOCAL);

    private final ResidenceRegistrationStore registrations;
    private final RegistrationSubmissionStore submissions;
    private final RegistrationResidentStore residents;
    private final ResidenceRegistrationService service;
    private final FileApi files;

    RegistrationCorrectionProvider(
            ResidenceRegistrationStore registrations,
            RegistrationSubmissionStore submissions,
            RegistrationResidentStore residents,
            ResidenceRegistrationService service,
            FileApi files) {
        this.registrations = registrations;
        this.submissions = submissions;
        this.residents = residents;
        this.service = service;
        this.files = files;
    }

    @Override
    public String recordType() {
        return RegistrationNames.ENTITY_TYPE;
    }

    /** Có nội dung đã ghi: lần nộp, hoặc xác nhận thường trú cùng xã/phường. */
    @Override
    public boolean isConfirmed(long id) {
        return registrations
                .find(id)
                .map(r -> r.currentSubmissionId() != null || r.localPermanentResident())
                .orElse(false);
    }

    /** Người ghi lần nộp hiện hành; chưa có lần nộp thì người ghi xác nhận thường trú cùng xã/phường. */
    @Override
    public long originalRecorder(long id) {
        ResidenceRegistration registration = find(id);
        if (registration.currentSubmissionId() != null) {
            return submissions
                    .find(registration.currentSubmissionId())
                    .orElseThrow()
                    .recordedBy();
        }
        return residents.confirmedBy(registration.residentId()).orElseThrow(NotFoundException::new);
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.OFFICE_STAFF);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException("Nội dung đúng chỉ gồm trường đã ghi của khai báo: " + FIELDS);
        }
        if (map.containsKey(LOCAL) && !Boolean.FALSE.equals(map.get(LOCAL))) {
            throw new InvalidValueException(
                    "Chỉ gỡ được xác nhận thường trú cùng xã/phường (localPermanentResident: false)");
        }
        for (String date : new String[] {"submittedDate", "expiryDate"}) {
            if (map.get(date) != null) {
                try {
                    LocalDate.parse(String.valueOf(map.get(date)));
                } catch (DateTimeParseException e) {
                    throw new InvalidValueException("Ngày phải theo ISO-8601: " + map.get(date));
                }
            }
        }
        if (map.get("channel") != null) {
            try {
                SubmissionChannel.valueOf(String.valueOf(map.get("channel")));
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException("Kênh nộp không hợp lệ: " + map.get("channel"));
            }
        }
        if (map.get("evidenceFileId") != null && !(map.get("evidenceFileId") instanceof Number)) {
            throw new InvalidValueException("evidenceFileId phải là số");
        }
    }

    @Override
    public Object originalContent(long id) {
        ResidenceRegistration registration = find(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put(LOCAL, registration.localPermanentResident());
        content.put("basisNote", registration.basisNote());
        if (registration.currentSubmissionId() != null) {
            RegistrationSubmission submission =
                    submissions.find(registration.currentSubmissionId()).orElseThrow();
            content.put("submittedDate", submission.submittedDate().toString());
            content.put("channel", submission.channel().name());
            content.put("receivingAuthority", submission.receivingAuthority());
            content.put("dossierCode", submission.dossierCode());
            content.put("result", submission.result().name());
            content.put("rejectionReason", submission.rejectionReason());
        }
        content.put(
                "expiryDate",
                registration.expiryDate() == null
                        ? null
                        : registration.expiryDate().toString());
        content.put("evidenceFileId", registration.evidenceFileId());
        return content;
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        if (correction.type() == CorrectionType.VOID) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "CORRECTION_NOT_SUPPORTED",
                    "Khai báo không Hủy ghi nhận được; sửa nội dung đã ghi (FR-005)");
        }
        Map<?, ?> content = (Map<?, ?>) correction.correctedContent();
        if (content.containsKey(LOCAL)) {
            service.removeLocalConfirmation(correction.originalRecordId());
        }
        if (content.get("evidenceFileId") instanceof Number evidence) {
            files.attach(evidence.longValue(), RegistrationNames.EVIDENCE_OWNER_TYPE, correction.originalRecordId());
        }
    }

    private ResidenceRegistration find(long id) {
        return registrations.find(id).orElseThrow(NotFoundException::new);
    }
}
