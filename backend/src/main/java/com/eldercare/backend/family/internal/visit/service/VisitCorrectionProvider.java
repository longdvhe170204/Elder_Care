package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.family.internal.visit.domain.Visit;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính lượt thăm ({@code DinhChinhDuocProvider} {@code M10.LUOT_THAM}; research F-08): lượt Đã ra đã khóa, chuỗi
 * đính chính của 000 ghi giờ vào, giờ ra đúng (dữ liệu truy vết của 007). Người thực tế vào không đính chính ở đây.
 */
@Component
class VisitCorrectionProvider implements CorrectableRecordProvider {

    private static final Set<String> FIELDS = Set.of("checkedInAt", "checkedOutAt");

    private final SimpleRecordApi records;
    private final PermissionApi permissions;

    VisitCorrectionProvider(SimpleRecordApi records, PermissionApi permissions) {
        this.records = records;
        this.permissions = permissions;
    }

    @Override
    public String recordType() {
        return Visit.KIND.code();
    }

    @Override
    public boolean isConfirmed(long id) {
        return visit(id).status().equals(Visit.Status.CHECKED_OUT.name());
    }

    @Override
    public long originalRecorder(long id) {
        Long by = visit(id).content().checkedOutBy();
        return by == null ? visit(id).createdBy() : by;
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> residentId(long id) {
        return Optional.of(visit(id).residentId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return permissions.permissions(user).contains(new PermissionKey(Function.VISIT_REGISTRATION, Operation.UPDATE));
    }

    @Override
    public void validateContent(Object correctedContent) {
        if (!(correctedContent instanceof Map<?, ?> map) || map.isEmpty() || !FIELDS.containsAll(map.keySet())) {
            throw new InvalidValueException("Chỉ đính chính giờ vào (checkedInAt) và giờ ra (checkedOutAt)");
        }
        for (Object value : map.values()) {
            try {
                Instant.parse(String.valueOf(value));
            } catch (DateTimeParseException e) {
                throw new InvalidValueException("Thời điểm chưa hợp lệ");
            }
        }
    }

    @Override
    public Object originalContent(long id) {
        Visit visit = visit(id).content();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("checkedInAt", String.valueOf(visit.checkedInAt()));
        content.put("checkedOutAt", String.valueOf(visit.checkedOutAt()));
        return content;
    }

    private SimpleRecord<Visit> visit(long id) {
        return records.require(Visit.KIND, id);
    }
}
