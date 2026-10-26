package com.cambae.backend.m02.internal.death.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m02.internal.death.domain.DeathRecord;
import com.cambae.backend.m02.internal.death.persistence.DeathStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính bản ghi qua đời (nhóm 3; FR-069, research T-13): bổ sung nguyên nhân và sửa nơi xảy ra, người phát hiện, người
 * xác nhận. Thời điểm qua đời và bằng chứng không sửa được ở đây vì kéo theo lượt vắng, hợp đồng và tính phí đã chốt theo
 * thời điểm đó. Không gắn tầng nên đi cách A (người ghi gốc) hoặc C (yêu cầu phê duyệt). Nội dung gốc đưa ra đây gồm cả
 * nguyên nhân; giới hạn theo nhóm sức khỏe do 000, 002 áp khi trả về cho người xem.
 */
@Component
class DeathRecordCorrectionProvider implements CorrectableRecordProvider {

    private static final Set<String> FIELDS = Set.of("cause", "place", "discoveredBy", "confirmedById");

    private final DeathStore store;

    DeathRecordCorrectionProvider(DeathStore store) {
        this.store = store;
    }

    @Override
    public String recordType() {
        return PostDeathTaskService.ENTITY_TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.find(id).isPresent();
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
        return user.roles().contains(Role.DOCTOR) || user.roles().contains(Role.OFFICE_STAFF);
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException(
                    "Nội dung đúng cần một hoặc nhiều trường cause, place, discoveredBy, confirmedById");
        }
        for (String text : new String[] {"cause", "place", "discoveredBy"}) {
            if (map.containsKey(text) && String.valueOf(map.get(text)).isBlank()) {
                throw new InvalidValueException(text + " không được để trống");
            }
        }
        if (map.containsKey("confirmedById") && !(map.get("confirmedById") instanceof Number n && n.longValue() > 0)) {
            throw new InvalidValueException("confirmedById phải là mã số nguyên dương");
        }
    }

    @Override
    public Object originalContent(long id) {
        DeathRecord record = find(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("place", record.place());
        content.put("discoveredBy", record.discoveredBy());
        content.put("confirmedById", record.confirmedById());
        if (record.causeOfDeath() != null) {
            content.put("cause", record.causeOfDeath());
        }
        return content;
    }

    private DeathRecord find(long id) {
        return store.find(id).orElseThrow(NotFoundException::new);
    }
}
