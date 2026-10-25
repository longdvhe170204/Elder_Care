package com.cambae.backend.m01.internal.health.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.events.InitialHealthRecordChanged;
import com.cambae.backend.m01.internal.health.domain.InitialHealthRecord;
import com.cambae.backend.m01.internal.health.persistence.InitialHealthRecordRepository;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.CorrectionView;
import com.cambae.backend.m15.correction.api.CreationMethod;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Đính chính hồ sơ sức khỏe ban đầu sau xác nhận (001 T029; research H-08; FR-017, FR-049a). Bản ghi không gắn tầng, nên
 * đi cách A (người ghi gốc) hoặc C (yêu cầu phê duyệt); trên hồ sơ trạng thái cuối chỉ cách C. Báo {@code
 * InitialHealthRecordChanged} để 006 đọc lại thuốc đang dùng khi tiếp nhận.
 */
@Component
class InitialHealthRecordCorrectionProvider implements CorrectableRecordProvider {

    private static final Set<String> FIELDS = Set.of(
            "bloodType",
            "mobility",
            "selfCareAbility",
            "cognition",
            "initialNeedsNote",
            "nutritionNeeds",
            "currentMedications",
            "rehabilitationNeeds");

    private final InitialHealthRecordRepository records;
    private final ResidentApi residents;
    private final ApplicationEventPublisher events;

    InitialHealthRecordCorrectionProvider(
            InitialHealthRecordRepository records, ResidentApi residents, ApplicationEventPublisher events) {
        this.records = records;
        this.residents = residents;
        this.events = events;
    }

    @Override
    public String recordType() {
        return InitialHealthRecordService.RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return records.findById(id).map(InitialHealthRecord::isConfirmed).orElse(false);
    }

    @Override
    public long originalRecorder(long id) {
        return find(id).getPerformedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.DOCTOR) || user.roles().contains(Role.NURSING_STAFF);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException("Nội dung đúng cần ít nhất một trường của hồ sơ sức khỏe ban đầu");
        }
    }

    @Override
    public Object originalContent(long id) {
        return InitialHealthRecordService.snapshot(find(id).content());
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        return method == CreationMethod.C || !residents.isFinalStatus(find(id).getResidentId());
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        events.publishEvent(new InitialHealthRecordChanged(
                find(correction.originalRecordId()).getResidentId()));
    }

    private InitialHealthRecord find(long id) {
        return records.findById(id).orElseThrow(NotFoundException::new);
    }
}
