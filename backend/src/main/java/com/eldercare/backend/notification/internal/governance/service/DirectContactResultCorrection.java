package com.eldercare.backend.notification.internal.governance.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Đính chính kết quả liên hệ trực tiếp ({@code M13.KET_QUA_LIEN_HE}; FR-021, FR-031, data-model mục 9): kết quả nhóm 3
 * không sửa, chỉ đính chính qua 000; người ghi là Hành chính; nội dung đúng có {@code result} và lý do khi Không liên hệ
 * được.
 */
@Component
class DirectContactResultCorrection implements CorrectableRecordProvider {

    static final String TYPE = "M13.DIRECT_CONTACT_RESULT";
    private static final Set<String> RESULTS = Set.of("CONTACTED", "UNREACHABLE");

    private final JdbcTemplate jdbc;

    DirectContactResultCorrection(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String recordType() {
        return TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return !jdbc.queryForList("SELECT id FROM direct_contact_result WHERE id = ?", Long.class, id)
                .isEmpty();
    }

    @Override
    public long originalRecorder(long id) {
        return jdbc.queryForObject("SELECT contacted_by FROM direct_contact_result WHERE id = ?", Long.class, id);
    }

    /** Liên hệ trực tiếp do Hành chính làm cho toàn viện, không gắn tầng. */
    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.empty();
    }

    /** Người cao tuổi của yêu cầu thông báo mà dòng liên hệ trực tiếp thuộc về. */
    @Override
    public Optional<Long> residentId(long id) {
        return jdbc
                .queryForList(
                        "SELECT q.resident_id FROM direct_contact_result r JOIN direct_contact d ON d.id = r.direct_contact_id"
                                + " JOIN notification n ON n.id = d.notification_id"
                                + " JOIN notification_request q ON q.id = n.request_id WHERE r.id = ?",
                        Long.class,
                        id)
                .stream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().contains(Role.OFFICE_STAFF);
    }

    @Override
    public void validateContent(Object content) {
        if (!(content instanceof Map<?, ?> map)
                || !(map.get("result") instanceof String result)
                || !RESULTS.contains(result)) {
            throw new InvalidValueException("Nội dung đúng cần trường result (CONTACTED, UNREACHABLE)");
        }
        if (result.equals("UNREACHABLE") && !(map.get("reason") instanceof String reason && !reason.isBlank())) {
            throw new InvalidValueException("Không liên hệ được cần lý do");
        }
    }

    @Override
    public Object originalContent(long id) {
        return jdbc.queryForMap(
                "SELECT result, reason, note, occurred_at AS occurredAt FROM direct_contact_result WHERE id = ?", id);
    }
}
