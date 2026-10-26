package com.cambae.backend.m02.internal.absence.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m02.internal.absence.domain.Absence;
import com.cambae.backend.m02.internal.absence.persistence.AbsenceStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CorrectionView;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính lượt vắng đã đóng (FR-061, BR-M02-18; research T-08): chỉ thời điểm rời viện, thời điểm về. Lượt vắng không gắn
 * tầng nên đi cách A (người ghi gốc) hoặc C (yêu cầu phê duyệt). Sau khi lưu, các ngày bị ảnh hưởng được tính lại và 010
 * được báo; ngày mới vào dòng "Quản lý quyết định" thì Quản lý viện được báo để đặt hệ số.
 */
@Component
class AbsenceCorrectionProvider implements CorrectableRecordProvider {

    private static final Set<String> FIELDS = Set.of("departedAt", "returnedAt");

    private final AbsenceStore store;
    private final AbsenceService service;

    AbsenceCorrectionProvider(AbsenceStore store, AbsenceService service) {
        this.store = store;
        this.service = service;
    }

    @Override
    public String recordType() {
        return AbsenceService.RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.find(id).map(absence -> !absence.open()).orElse(false);
    }

    @Override
    public long originalRecorder(long id) {
        Absence absence = find(id);
        return absence.closedBy() != null ? absence.closedBy() : absence.createdBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.OFFICE_STAFF) || user.roles().contains(Role.FLOOR_MANAGER);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException("Nội dung đúng cần departedAt hoặc returnedAt của lượt vắng");
        }
        map.values().forEach(AbsenceCorrectionProvider::instant);
    }

    @Override
    public Object originalContent(long id) {
        Absence absence = find(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("departedAt", absence.departedAt().toString());
        content.put(
                "returnedAt",
                absence.returnedAt() == null ? null : absence.returnedAt().toString());
        return content;
    }

    @Override
    public void afterCorrection(CorrectionView correction) {
        service.recalculateAfterCorrection(correction.originalRecordId(), correction.type() == CorrectionType.VOID);
    }

    private Absence find(long id) {
        return store.find(id).orElseThrow(NotFoundException::new);
    }

    private static Instant instant(Object value) {
        try {
            return Instant.parse(String.valueOf(value));
        } catch (DateTimeParseException e) {
            throw new InvalidValueException("Thời điểm phải theo ISO-8601: " + value);
        }
    }
}
