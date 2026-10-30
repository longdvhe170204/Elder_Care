package com.eldercare.backend.administration.sample;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.spi.CorrectableRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Provider đính chính cho bản ghi mẫu gắn tầng (000 T060). Bản ghi mẫu không có bản nháp nên tồn tại là đã xác nhận. */
@Component
public class SampleFloorRecordProvider implements CorrectableRecordProvider {

    public static final String TYPE = "M15.SAMPLE_RECORD";

    private final JdbcTemplate jdbc;

    public SampleFloorRecordProvider(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String recordType() {
        return TYPE;
    }

    @Override
    public boolean isConfirmed(long id) {
        return !jdbc.queryForList("SELECT id FROM sample_floor_record WHERE id = ?", Long.class, id)
                .isEmpty();
    }

    @Override
    public long originalRecorder(long id) {
        return jdbc.queryForObject("SELECT actor_id FROM sample_floor_record WHERE id = ?", Long.class, id);
    }

    @Override
    public Optional<Long> floorArea(long id) {
        return Optional.of(
                jdbc.queryForObject("SELECT floor_area_id FROM sample_floor_record WHERE id = ?", Long.class, id));
    }

    @Override
    public boolean canRecord(CurrentUser user) {
        return user.roles().stream()
                .anyMatch(r -> List.of(Role.NURSING_STAFF, Role.CAREGIVER).contains(r));
    }

    @Override
    public void validateContent(Object content) {
        if (!(content instanceof Map<?, ?> map) || !(map.get("note") instanceof String)) {
            throw new InvalidValueException("Nội dung đúng cần trường note");
        }
    }

    @Override
    public Object originalContent(long id) {
        return Map.of(
                "note", jdbc.queryForObject("SELECT note FROM sample_floor_record WHERE id = ?", String.class, id));
    }
}
