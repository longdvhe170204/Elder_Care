package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính sự cố (FR-045, FR-046, FR-046a; BR-M05-21; T036): {@code M05.INCIDENT} cho Hủy ghi nhận và đổi loại,
 * {@code M05.INCIDENT_PROGRESSION} cho diễn biến. Tác động của loại mới (Ngã, lây nhiễm) thuộc T047, T052.
 */
public final class IncidentCorrectionProviders {
    public static final String INCIDENT = "M05.INCIDENT";
    public static final String PROGRESSION = "M05.INCIDENT_PROGRESSION";
    private static final Set<Role> RECORDERS = Set.of(Role.NURSING_STAFF, Role.FLOOR_MANAGER, Role.DOCTOR);

    private IncidentCorrectionProviders() {}

    @Component
    static class IncidentProvider implements CorrectableRecordProvider {
        private static final Set<Role> EMERGENCY_ROLES = Set.of(Role.NURSING_STAFF, Role.DOCTOR);

        private final IncidentStore incidents;
        private final IncidentCatalogService catalog;
        private final ContainmentStore containments;
        private final IncidentCorrectionEffects effects;

        IncidentProvider(
                IncidentStore incidents,
                IncidentCatalogService catalog,
                ContainmentStore containments,
                IncidentCorrectionEffects effects) {
            this.incidents = incidents;
            this.catalog = catalog;
            this.containments = containments;
            this.effects = effects;
        }

        @Override
        public String recordType() {
            return INCIDENT;
        }

        @Override
        public boolean isConfirmed(long id) {
            return incidents.view(id).isPresent();
        }

        @Override
        public long originalRecorder(long id) {
            return find(id).recordedBy();
        }

        @Override
        public Optional<Long> floorArea(long id) {
            return Optional.ofNullable(find(id).floorAreaId());
        }

        @Override
        public Optional<Long> residentId(long id) {
            return Optional.ofNullable(find(id).residentId());
        }

        @Override
        public boolean canRecord(CurrentUser user) {
            return RECORDERS.stream().anyMatch(user.roles()::contains);
        }

        @Override
        public void validateContent(Object correctedContent) {
            if (!(correctedContent instanceof Map<?, ?> map)
                    || map.size() != 1
                    || !(map.get("incidentTypeId") instanceof Number number)) {
                throw new InvalidValueException("Nội dung đúng chỉ gồm incidentTypeId");
            }
            catalog.incidentTypes().stream()
                    .filter(type -> type.id() == number.longValue() && type.active())
                    .findFirst()
                    .orElseThrow(() -> new InvalidValueException("Loại sự cố không tồn tại hoặc đã ngừng dùng"));
        }

        @Override
        public Object originalContent(long id) {
            var content = new LinkedHashMap<String, Object>();
            content.put("incidentTypeId", find(id).currentTypeId());
            return content;
        }

        /** BR-M05-21: sự cố từng Khẩn cấp chỉ Điều dưỡng, Bác sĩ; không bỏ sự cố lây nhiễm còn vùng chỉ gắn nó. */
        @Override
        public boolean allows(CurrentUser user, CreationMethod method, long id) {
            var incident = find(id);
            if (incident.wasEmergency() && EMERGENCY_ROLES.stream().noneMatch(user.roles()::contains)) return false;
            return !"SUSPECTED_INFECTION".equals(incidents.currentTypeCode(id)) || !containments.activeOnlyFor(id);
        }

        @Override
        public void afterCorrection(CorrectionView correction) {
            long id = correction.originalRecordId();
            var before = find(id);
            if (correction.type() == CorrectionType.VOID) {
                incidents.cancel(id);
                effects.voided(before, correction.id());
                return;
            }
            String oldType = incidents.currentTypeCode(id);
            long typeId = ((Number) ((Map<?, ?>) correction.correctedContent()).get("incidentTypeId")).longValue();
            var type = catalog.incidentTypes().stream()
                    .filter(candidate -> candidate.id() == typeId)
                    .findFirst()
                    .orElseThrow(() -> new InvalidValueException("Loại sự cố không tồn tại"));
            incidents.changeType(id, type.id(), type.code());
            effects.typeChanged(before, oldType, type.code(), correction.actorId(), correction.id());
        }

        private IncidentView find(long id) {
            return incidents.view(id).orElseThrow(NotFoundException::new);
        }
    }

    @Component
    static class ProgressionProvider implements CorrectableRecordProvider {
        private final IncidentProgressionStore progressions;

        ProgressionProvider(IncidentProgressionStore progressions) {
            this.progressions = progressions;
        }

        @Override
        public String recordType() {
            return PROGRESSION;
        }

        @Override
        public boolean isConfirmed(long id) {
            return true;
        }

        @Override
        public long originalRecorder(long id) {
            return progressions.get(id).actorId();
        }

        @Override
        public Optional<Long> floorArea(long id) {
            return Optional.ofNullable(progressions.get(id).floorAreaId());
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
            return Map.of("content", progressions.get(id).content().content());
        }
    }
}
