package com.eldercare.backend.resident.internal.assessment.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.internal.assessment.domain.Assessment;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.persistence.AssessmentRepository;
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
    private final ResidentApi residents;
    private final ApplicationEventPublisher events;

    AssessmentCorrectionProvider(
            AssessmentRepository assessments,
            AssessmentContents contents,
            ResidentApi residents,
            ApplicationEventPublisher events) {
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

    private Assessment find(long id) {
        return assessments.findById(id).orElseThrow(NotFoundException::new);
    }
}
