package com.eldercare.backend.notification.internal.urgent.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.CreationMethod;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.error.InvalidValueException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private final JdbcTemplate jdbc;
    private final CallReminderRouting routing;

    CallResultCorrection(JdbcTemplate jdbc, CallReminderRouting routing) {
        this.jdbc = jdbc;
        this.routing = routing;
    }

    @Override
    public String recordType() {
        return TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return !jdbc.queryForList("SELECT id FROM call_result WHERE id = ?", Long.class, id)
                .isEmpty();
    }

    @Override
    public long originalRecorder(long id) {
        return jdbc.queryForObject("SELECT called_by FROM call_result WHERE id = ?", Long.class, id);
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return jdbc
                .queryForList(
                        "SELECT r.floor_area_id FROM call_result c JOIN call_reminder r ON r.id = c.call_reminder_id"
                                + " WHERE c.id = ?",
                        Long.class,
                        id)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    /** Người cao tuổi của lượt nhắc gọi; lượt không gắn người cao tuổi (FR-027a) thì rỗng. */
    @Override
    public Optional<Long> residentId(long id) {
        return jdbc
                .queryForList(
                        "SELECT r.resident_id FROM call_result c JOIN call_reminder r ON r.id = c.call_reminder_id"
                                + " WHERE c.id = ?",
                        Long.class,
                        id)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
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
        return jdbc.queryForMap(
                "SELECT result, reason, note, answered_by AS answeredBy, called_at AS calledAt,"
                        + " called_phone_number AS calledPhoneNumber FROM call_result WHERE id = ?",
                id);
    }
}
