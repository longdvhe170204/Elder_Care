package com.eldercare.backend.incident.internal.critical.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.critical.domain.ExecutionChoice;
import com.eldercare.backend.incident.internal.critical.domain.ReconfirmationResult;
import com.eldercare.backend.incident.internal.critical.persistence.CriticalCareStore;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongFunction;
import org.springframework.stereotype.Component;

/**
 * Đính chính kết quả xác nhận lại ({@code M05.WISH_RECONFIRMATION}) và lựa chọn thực hiện ({@code M05.CHOICE_EXECUTION})
 * qua 000 (FR-050g, research S-13). Người ghi là Bác sĩ hoặc Điều dưỡng như lệnh gốc.
 */
public final class CriticalCareCorrectionProviders {
    private static final Set<Role> RECORDERS = Set.of(Role.DOCTOR, Role.NURSING_STAFF);

    private CriticalCareCorrectionProviders() {}

    @Component
    static class ReconfirmationProvider extends Provider {
        ReconfirmationProvider(CriticalCareStore store, AlertStore alerts) {
            super(CriticalCareStore.RECONFIRMATION_TYPE, store::reconfirmation, alerts);
        }

        @Override
        public void validateContent(Object correctedContent) {
            requireEnum(correctedContent, "result", ReconfirmationResult.class);
        }
    }

    @Component
    static class ChoiceProvider extends Provider {
        ChoiceProvider(CriticalCareStore store, AlertStore alerts) {
            super(CriticalCareStore.CHOICE_TYPE, store::choice, alerts);
        }

        @Override
        public void validateContent(Object correctedContent) {
            requireEnum(correctedContent, "choice", ExecutionChoice.class);
        }
    }

    abstract static class Provider implements CorrectableRecordProvider {
        private final String type;
        private final LongFunction<? extends RecordEvent<?>> rows;
        private final AlertStore alerts;

        Provider(String type, LongFunction<? extends RecordEvent<?>> rows, AlertStore alerts) {
            this.type = type;
            this.rows = rows;
            this.alerts = alerts;
        }

        @Override
        public String recordType() {
            return type;
        }

        @Override
        public boolean isConfirmed(long id) {
            return rows.apply(id).confirmedAt() != null;
        }

        @Override
        public long originalRecorder(long id) {
            return rows.apply(id).actorId();
        }

        @Override
        public Optional<Long> floorArea(long id) {
            return Optional.ofNullable(rows.apply(id).floorAreaId());
        }

        @Override
        public Optional<Long> residentId(long id) {
            return alerts.findById(rows.apply(id).parent().entityId()).map(AlertRecord::residentId);
        }

        @Override
        public boolean canRecord(CurrentUser user) {
            return RECORDERS.stream().anyMatch(user.roles()::contains);
        }

        @Override
        public Object originalContent(long id) {
            return rows.apply(id).content();
        }

        static <E extends Enum<E>> void requireEnum(Object content, String field, Class<E> type) {
            if (!(content instanceof Map<?, ?> map) || !(map.get(field) instanceof String value)) {
                throw new InvalidValueException("Nội dung đúng cần " + field);
            }
            try {
                Enum.valueOf(type, value);
            } catch (IllegalArgumentException e) {
                throw new InvalidValueException("Giá trị " + field + " không hợp lệ");
            }
        }
    }
}
