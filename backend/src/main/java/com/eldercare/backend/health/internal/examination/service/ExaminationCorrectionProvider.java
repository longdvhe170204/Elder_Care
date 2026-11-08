package com.eldercare.backend.health.internal.examination.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính bản ghi khám và điều trị {@code M06.EXAMINATION_RECORD} qua 000 (FR-074): bản gốc không sửa; nội dung đúng
 * là {@code {"content": "..."}}. Người ghi là Bác sĩ hoặc Điều dưỡng như lệnh gốc.
 */
@Component
public class ExaminationCorrectionProvider implements CorrectableRecordProvider {
    private static final Set<Role> RECORDERS = Set.of(Role.DOCTOR, Role.NURSING_STAFF);

    private final RecordEventApi records;

    public ExaminationCorrectionProvider(RecordEventApi records) {
        this.records = records;
    }

    @Override
    public String recordType() {
        return ExaminationService.RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return records.require(ExaminationService.KIND, id).confirmedAt() != null;
    }

    @Override
    public long originalRecorder(long id) {
        return records.require(ExaminationService.KIND, id).actorId();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.ofNullable(records.require(ExaminationService.KIND, id).floorAreaId());
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(records.require(ExaminationService.KIND, id).parent().entityId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return RECORDERS.stream().anyMatch(user.roles()::contains);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map)
                || !(map.get("content") instanceof String text)
                || text.isBlank()) {
            throw new InvalidValueException("Nội dung đúng cần content");
        }
    }

    @Override
    public Object originalContent(long id) {
        return Map.of(
                "content",
                records.require(ExaminationService.KIND, id).content().get("content"));
    }
}
