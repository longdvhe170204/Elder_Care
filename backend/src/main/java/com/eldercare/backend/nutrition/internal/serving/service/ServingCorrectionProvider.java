package com.eldercare.backend.nutrition.internal.serving.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.nutrition.internal.serving.domain.ServingConfirmation;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Đính chính xác nhận phục vụ qua feature 000 (FR-051; {@code M08.SERVING_CONFIRMATION}). */
@Component
class ServingCorrectionProvider implements CorrectableRecordProvider {

    private final RecordEventApi events;

    ServingCorrectionProvider(RecordEventApi events) {
        this.events = events;
    }

    @Override
    public String recordType() {
        return ServingConfirmation.KIND.code();
    }

    @Override
    public boolean isConfirmed(long id) {
        try {
            return row(id).confirmedAt() != null;
        } catch (NotFoundException missing) {
            return false;
        }
    }

    @Override
    public long originalRecorder(long id) {
        return row(id).actorId();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.of(row(id).content().floorAreaId());
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(row(id).content().residentId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.CAREGIVER) || user.roles().contains(Role.NURSING_STAFF);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> content) || !(content.get("result") instanceof String result)) {
            throw new InvalidValueException("Nội dung đúng của xác nhận phục vụ phải nêu kết quả");
        }
        try {
            ServingConfirmation.Result.valueOf(result);
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("Kết quả xác nhận phục vụ không hợp lệ");
        }
    }

    @Override
    public Object originalContent(long id) {
        ServingConfirmation c = row(id).content();
        return Map.of(
                "specialPortionId", c.specialPortionId(),
                "residentId", c.residentId(),
                "result", c.result().name());
    }

    private RecordEvent<ServingConfirmation> row(long id) {
        return events.require(ServingConfirmation.KIND, id);
    }
}
