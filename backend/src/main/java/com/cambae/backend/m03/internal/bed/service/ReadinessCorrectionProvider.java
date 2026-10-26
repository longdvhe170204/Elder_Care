package com.cambae.backend.m03.internal.bed.service;

import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m03.internal.bed.persistence.ReadinessCorrectionStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
import java.util.*;
import org.springframework.stereotype.Component;

/** FR-044: chỉ đính chính sự kiện CONFIRM_READY và ba trường ghi nhận, không đổi trạng thái giường. */
@Component
public class ReadinessCorrectionProvider implements CorrectableRecordProvider {
    private final ReadinessCorrectionStore store;
    private final PermissionApi permissions;
    private final ScopeApi scope;

    public ReadinessCorrectionProvider(ReadinessCorrectionStore store, PermissionApi permissions, ScopeApi scope) {
        this.store = store;
        this.permissions = permissions;
        this.scope = scope;
    }

    @Override
    public String recordType() {
        return "M03.BED_READINESS_CONFIRMATION";
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.find(id).map(row -> row.event().equals("CONFIRM_READY")).orElse(false);
    }

    @Override
    public long originalRecorder(long id) {
        var actor = store.required(id).actorId();
        return actor == null ? 0 : actor;
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.of(store.required(id).floorId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        var key = new PermissionKey(Function.BED_READINESS, Operation.CREATE);
        var filter = scope.filter(user, key);
        if (filter.facilityWide())
            return permissions
                    .hasPermission(user, key, new Target(null, null, null, null))
                    .allowed();
        return filter.floorAreaIds().stream()
                .anyMatch(floor -> permissions
                        .hasPermission(user, key, new Target(null, null, null, floor))
                        .allowed());
    }

    @Override
    public boolean allows(CurrentUser user, com.cambae.backend.m15.correction.api.CreationMethod method, long id) {
        return method == com.cambae.backend.m15.correction.api.CreationMethod.B
                || permissions
                        .hasPermission(
                                user,
                                new PermissionKey(Function.BED_READINESS, Operation.CREATE),
                                new Target(
                                        recordType(),
                                        Long.toString(id),
                                        null,
                                        store.required(id).floorId()))
                        .allowed();
    }

    @Override
    public Object originalContent(long id) {
        var row = store.required(id);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("note", row.note());
        content.put("disinfected", row.disinfected());
        content.put("ppeUsed", row.ppeUsed());
        return content;
    }

    @Override
    public void validateContent(Object value) {
        if (!(value instanceof Map<?, ?> map)
                || map.isEmpty()
                || !Set.of("note", "disinfected", "ppeUsed").containsAll(map.keySet()))
            throw new InvalidValueException("Chỉ đính chính ghi chú, khử khuẩn, bảo hộ");
        for (String key : List.of("disinfected", "ppeUsed"))
            if (map.containsKey(key) && !(map.get(key) instanceof Boolean))
                throw new InvalidValueException("Khử khuẩn, bảo hộ phải là boolean");
        if (map.containsKey("note")
                && map.get("note") != null
                && (!(map.get("note") instanceof String note) || note.length() > 2000))
            throw new InvalidValueException("Ghi chú tối đa 2000 ký tự");
    }
}
