package com.eldercare.backend.family.internal.pickup.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.family.internal.pickup.domain.PickupRecord;
import com.eldercare.backend.family.internal.pickup.persistence.PickupRecordStore;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính bản ghi đón ({@code DinhChinhDuocProvider} {@code M10.BAN_GHI_DON}; FR-026, nhóm 3): bản gốc không sửa, chuỗi
 * đính chính của 000 ghi thời điểm bàn giao, nhân viên bàn giao đúng. Người, căn cứ, giấy tờ đã đối chiếu không đính chính:
 * sai thì lượt đón không hợp lệ, phải ghi lượt mới.
 */
@Component
class PickupCorrectionProvider implements CorrectableRecordProvider {

    private static final Set<String> FIELDS = Set.of("at", "handoverStaffId");

    private final PickupRecordStore store;
    private final PermissionApi permissions;

    PickupCorrectionProvider(PickupRecordStore store, PermissionApi permissions) {
        this.store = store;
        this.permissions = permissions;
    }

    @Override
    public String recordType() {
        return PickupRecord.ENTITY_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.find(id).isPresent();
    }

    @Override
    public long originalRecorder(long id) {
        return record(id).recordedBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(record(id).residentId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return permissions.permissions(user).contains(new PermissionKey(Function.PICKUP, Operation.CREATE));
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException(
                    "Chỉ đính chính thời điểm bàn giao (at) và nhân viên bàn giao (handoverStaffId)");
        }
        if (map.get("at") != null) {
            try {
                Instant.parse(String.valueOf(map.get("at")));
            } catch (DateTimeParseException e) {
                throw new InvalidValueException("Thời điểm bàn giao chưa hợp lệ");
            }
        }
        if (map.containsKey("handoverStaffId") && !(map.get("handoverStaffId") instanceof Number)) {
            throw new InvalidValueException("Nhân viên bàn giao chưa hợp lệ");
        }
    }

    @Override
    public Object originalContent(long id) {
        PickupRecord record = record(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("at", record.at().toString());
        content.put("handoverStaffId", record.handoverStaffId());
        return content;
    }

    private PickupRecord record(long id) {
        return store.find(id).orElseThrow(() -> new InvalidValueException("Không có bản ghi đón " + id));
    }
}
