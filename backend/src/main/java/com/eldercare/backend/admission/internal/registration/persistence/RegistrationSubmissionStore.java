package com.eldercare.backend.admission.internal.registration.persistence;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationSubmission;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionContent;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionResult;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.NotFoundException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lần nộp ở {@code record_event} loại {@code M02.REGISTRATION_SUBMISSION}, cha là hồ sơ người cao tuổi (nhóm 3, FR-003; data-model
 * 018 mục 0.1; R-15): mỗi lần ghi đã nộp một dòng, không xóa (trigger của bảng chung); dòng còn là bản nháp tới khi đặt kết
 * quả, kết quả đặt một lần từ {@code PENDING} rồi dòng được xác nhận (khóa). Thời điểm lưu UTC (DBR-25). Từ Q-414 không còn
 * lệnh ghi đã nộp: chỉ đọc lần nộp cũ và đặt kết quả Được chấp nhận cho lần nộp còn {@code PENDING}.
 */
@Repository
public class RegistrationSubmissionStore {

    private static final String SOURCE = "M01.RESIDENT";

    private final RecordEventApi events;

    RegistrationSubmissionStore(RecordEventApi events) {
        this.events = events;
    }

    public Optional<RegistrationSubmission> find(long id) {
        try {
            return Optional.of(map(events.require(SubmissionContent.KIND, id)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /** Các lần nộp của một khai báo, theo thứ tự ghi (api-018 {@code submissions[]}). */
    public List<RegistrationSubmission> ofRegistration(long residentId, long registrationId) {
        return events.list(SubmissionContent.KIND, parent(residentId)).stream()
                .filter(row -> row.content().registrationId() == registrationId)
                .map(RegistrationSubmissionStore::map)
                .sorted(Comparator.comparingLong(RegistrationSubmission::id))
                .toList();
    }

    /** Đặt kết quả; {@code false} nếu lần nộp đã có kết quả. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean setResult(long id, SubmissionResult result, String rejectionReason, Instant at) {
        RecordEvent<SubmissionContent> row = events.require(SubmissionContent.KIND, id);
        if (row.confirmedAt() != null || row.content().result() != SubmissionResult.PENDING) {
            return false;
        }
        SubmissionContent c = row.content();
        try {
            events.updateDraft(
                    SubmissionContent.KIND,
                    id,
                    new SubmissionContent(
                            c.registrationId(),
                            c.submittedDate(),
                            c.channel(),
                            c.receivingAuthority(),
                            c.dossierCode(),
                            result,
                            rejectionReason,
                            c.recordedBy(),
                            c.recordedAt()));
            events.confirm(SubmissionContent.KIND, id, at);
        } catch (ConditionNotMetException e) {
            return false;
        }
        return true;
    }

    private static EntityRef parent(long residentId) {
        return new EntityRef(SOURCE, residentId);
    }

    private static RegistrationSubmission map(RecordEvent<SubmissionContent> row) {
        SubmissionContent c = row.content();
        return new RegistrationSubmission(
                row.id(),
                c.registrationId(),
                c.submittedDate(),
                c.channel(),
                c.receivingAuthority(),
                c.dossierCode(),
                c.result(),
                c.rejectionReason(),
                c.recordedBy(),
                c.recordedAt());
    }
}
