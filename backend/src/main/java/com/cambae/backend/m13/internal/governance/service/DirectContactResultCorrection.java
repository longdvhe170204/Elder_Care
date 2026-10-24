package com.cambae.backend.m13.internal.governance.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.spi.CorrectableRecordProvider;
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
