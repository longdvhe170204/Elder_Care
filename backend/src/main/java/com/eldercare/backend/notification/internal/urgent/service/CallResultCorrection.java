package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.notification.internal.urgent.domain.CallResultRow;
import com.eldercare.backend.notification.internal.urgent.persistence.CallReminderStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Đính chính kết quả gọi ({@code M13.KET_QUA_GOI}; FR-030, FR-031, data-model mục 6): kết quả nhóm 3 không sửa, chỉ đính
 * chính qua 000. Người ghi nhận là nhân viên; người được đính chính là người có quyền gọi của lượt lúc đính chính (FR-029).
 * Nội dung đúng có {@code result}, lý do khi Không liên lạc được, ghi chú khi Đã liên lạc được.
 */
@Component
class CallResultCorrection implements CorrectableRecordProvider {

    static final String TYPE = "M13.CALL_RESULT";
    private static final Set<String> REASONS = Set.of("NO_ANSWER", "WRONG_NUMBER", "NO_NUMBER", "OTHER");

    private final CallReminderStore store;
    private final CallReminderRouting routing;

    CallResultCorrection(CallReminderStore store, CallReminderRouting routing) {
        this.store = store;
        this.routing = routing;
    }

    @Override
    public String recordType() {
        return TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return store.callResult(id).isPresent();
    }

    @Override
    public long originalRecorder(long id) {
        return store.callResult(id).orElseThrow().calledBy();
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return store.callResultScope(id).map(s -> s.floorAreaId());
    }

    /** Người cao tuổi của lượt nhắc gọi; lượt không gắn người cao tuổi (FR-027a) thì rỗng. */
    @Override
    public Optional<Long> residentId(long id) {
        return store.callResultScope(id).map(s -> s.residentId());
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.staffId().isPresent();
    }

    @Override
    public boolean allows(CurrentUser user, CreationMethod method, long id) {
        return routing.canCall(user, floorArea(id).orElse(null));
    }

    @Override
    public void validateContent(Object content) {
        if (!(content instanceof Map<?, ?> map)
                || !(map.get("result") instanceof String result)
                || !Set.of("REACHED", "NOT_REACHED").contains(result)) {
            throw new InvalidValueException("Nội dung đúng cần trường result (REACHED, NOT_REACHED)");
        }
        if (result.equals("NOT_REACHED") && !(map.get("reason") instanceof String reason && REASONS.contains(reason))) {
            throw new InvalidValueException(
                    "Không liên lạc được cần lý do (NO_ANSWER, WRONG_NUMBER, NO_NUMBER, OTHER)");
        }
        if (result.equals("REACHED") && !(map.get("note") instanceof String note && !note.isBlank())) {
            throw new InvalidValueException("Đã liên lạc được cần ghi chú");
        }
    }

    @Override
    public Object originalContent(long id) {
        CallResultRow r = store.callResult(id).orElseThrow();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("result", r.result());
        content.put("reason", r.reason());
        content.put("note", r.note());
        content.put("answeredBy", r.answeredBy());
        content.put("calledAt", r.calledAt());
        content.put("calledPhoneNumber", r.calledPhoneNumber());
        return content;
    }
}
