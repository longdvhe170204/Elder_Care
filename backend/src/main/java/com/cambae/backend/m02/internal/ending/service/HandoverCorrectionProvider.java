package com.cambae.backend.m02.internal.ending.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m02.internal.ending.domain.Handover;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính lần bàn giao người cao tuổi (nhóm 3; FR-063 (e), research T-13): sửa thời điểm, nhân viên bàn giao, người thân
 * hoặc cơ sở tiếp nhận; Hủy ghi nhận thì điều kiện (e) tính lại ({@link EndingHandovers}). Không gắn tầng nên đi cách A
 * (người ghi gốc) hoặc C (yêu cầu phê duyệt).
 */
@Component
class HandoverCorrectionProvider implements CorrectableRecordProvider {

    private static final Set<String> FIELDS = Set.of("handoverAt", "handoverStaffId", "familyMemberId", "facilityName");

    private final ResidencyEndingStore store;

    HandoverCorrectionProvider(ResidencyEndingStore store) {
        this.store = store;
    }

    @Override
    public String recordType() {
        return EndingHandovers.RECORD_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.findHandover(id).isPresent();
    }

    @Override
    public long originalRecorder(long id) {
        return find(id).recordedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.OFFICE_STAFF) || user.roles().contains(Role.DOCTOR);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException(
                    "Nội dung đúng cần một hoặc nhiều trường handoverAt, handoverStaffId, familyMemberId, facilityName");
        }
        if (map.containsKey("handoverAt")) {
            try {
                Instant.parse(String.valueOf(map.get("handoverAt")));
            } catch (DateTimeParseException e) {
                throw new InvalidValueException("handoverAt phải theo ISO-8601");
            }
        }
        for (String id : new String[] {"handoverStaffId", "familyMemberId"}) {
            if (map.containsKey(id) && !(map.get(id) instanceof Number n && n.longValue() > 0)) {
                throw new InvalidValueException(id + " phải là mã số nguyên dương");
            }
        }
        if (map.containsKey("facilityName")
                && String.valueOf(map.get("facilityName")).isBlank()) {
            throw new InvalidValueException("facilityName không được để trống");
        }
    }

    @Override
    public Object originalContent(long id) {
        Handover handover = find(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("handoverAt", handover.handoverAt().toString());
        content.put("handoverStaffId", handover.handoverStaffId());
        if (handover.familyMemberId() != null) {
            content.put("familyMemberId", handover.familyMemberId());
        }
        if (handover.facilityName() != null) {
            content.put("facilityName", handover.facilityName());
        }
        return content;
    }

    private Handover find(long id) {
        return store.findHandover(id).orElseThrow(NotFoundException::new);
    }
}
