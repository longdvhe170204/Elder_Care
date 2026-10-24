package com.cambae.backend.m01.internal.assessment.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.internal.assessment.domain.Assessment;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentStatus;
import com.cambae.backend.m01.internal.assessment.persistence.AssessmentRepository;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.m15.correction.api.CorrectionView;
import com.cambae.backend.m15.correction.api.CreationMethod;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Đính chính lần đánh giá Đã xác nhận (001 T040; research H-10; FR-032, FR-049a). Người ghi gốc là Bác sĩ xác nhận,
 * không gắn tầng nên đi cách A hoặc C; trên hồ sơ trạng thái cuối chỉ cách C. Chỉ đính chính được nhận xét, lưu ý chăm
 * sóc, mức. Đính chính không đổi mức đã áp dụng; nếu đổi mức của lần Đã xác nhận gần nhất thì phát
 * {@link AssessmentCorrectedLevel}.
 */
@Component
class AssessmentCorrectionProvider implements CorrectableRecordProvider {

    private static final Set<String> FIELDS =
            Set.of(AssessmentContents.COMMENT, AssessmentContents.CARE_NOTE, AssessmentContents.CARE_LEVEL);

    private final AssessmentRepository assessments;
    private final AssessmentContents contents;
    private static final AuditAction CORRECTED = new AuditAction("RECORD_CORRECTED", true);

    private final ResidentApi residents;
    private final ApplicationEventPublisher events;
    private final AuditLogApi auditLog;

    AssessmentCorrectionProvider(
            AssessmentRepository assessments,
            AssessmentContents contents,
            ResidentApi residents,
            ApplicationEventPublisher events,
            AuditLogApi auditLog) {
        this.auditLog = auditLog;
        this.assessments = assessments;
        this.contents = contents;
        this.residents = residents;
        this.events = events;
    }

    @Override
    public String recordType() {
        return AssessmentService.RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return assessments.findById(id).map(Assessment::isConfirmed).orElse(false);
    }

    @Override
    public long originalRecorder(long id) {
        return find(id).getConfirmedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.DOCTOR);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException(
                    "Nội dung đúng cần ít nhất một trong các trường comment, careNote, careLevel");
        }
        // Form đính chính dùng chung gửi mọi ô: nhận xét và mức để trống nghĩa là giữ nguyên (không được rỗng, DBR-05);
        // lưu ý chăm sóc để trống nghĩa là bỏ lưu ý.
        Object level = map.get(AssessmentContents.CARE_LEVEL);
        if (level != null && !level.toString().isBlank()) {
            try {
                CareLevel.valueOf(level.toString().strip());
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException("Mức chăm sóc không hợp lệ (BASIC, REGULAR hoặc SPECIAL)");
            }
        }
    }

    @Override
    public Object originalContent(long id) {
        Assessment a = find(id);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(AssessmentContents.COMMENT, a.getComment());
        values.put(AssessmentContents.CARE_NOTE, a.getCareNote());
        values.put(AssessmentContents.CARE_LEVEL, a.getCareLevel().name());
        return values;
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        return method == CreationMethod.C || !residents.isFinalStatus(find(id).getResidentId());
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        Assessment a = find(correction.originalRecordId());
        linkToResident(correction, a.getResidentId());
        boolean latest = assessments
                .findFirstByResidentIdAndStatusOrderByConfirmedAtDescIdDesc(
                        a.getResidentId(), AssessmentStatus.CONFIRMED)
                .map(l -> l.getId().equals(a.getId()))
                .orElse(false);
        if (latest
                && !residents.isFinalStatus(a.getResidentId())
                && contents.of(a).careLevel() != a.getCareLevel()) {
            events.publishEvent(new AssessmentCorrectedLevel(a.getResidentId(), a.getId(), correction.id()));
        }
    }

    /**
     * Dòng nhật ký của 000 cho đính chính không mang {@code resident_id} (SPI không cho biết người cao tuổi), nên tra
     * theo người cao tuổi sẽ không thấy đính chính. Ghi thêm một dòng cùng bản ghi gốc, gắn người cao tuổi, trỏ tới
     * bản đính chính (DBR-23); không chép nội dung đính chính vì có thể là dữ liệu sức khỏe.
     */
    private void linkToResident(CorrectionView correction, long residentId) {
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(CORRECTED)
                .entity(correction.recordType(), correction.originalRecordId())
                .resident(residentId)
                .reason(new Reason(correction.reason()))
                .details(Map.of(
                        "correctionId", correction.id(),
                        "sequenceNo", correction.sequenceNo(),
                        "type", correction.type().name()));
        if (correction.approvalRequestId() != null) {
            entry.basis(new Basis(null, correction.approvalRequestId(), null, null));
        }
        auditLog.write(entry.build());
    }

    private Assessment find(long id) {
        return assessments.findById(id).orElseThrow(NotFoundException::new);
    }
}
