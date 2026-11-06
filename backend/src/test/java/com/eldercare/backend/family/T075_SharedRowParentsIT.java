package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 012 T075 (research R-15 mục 2, data-model mục 0.1): dòng bảng chung gắn một dòng {@code simple_record} của 012 (gia hạn,
 * dấu đêm của lượt ở lại; dấu nhắc của yêu cầu quyền) có {@code entity_type} trùng {@code dataType()} của một
 * {@link RetentionProvider}, để job loại bỏ dữ liệu xóa chúng cùng chuỗi, không để sót khi hồ sơ người cao tuổi hết hạn.
 */
class T075_SharedRowParentsIT extends MySqlIntegrationTestBase {

    private static final String STAY = "M10.FAMILY_STAY";
    private static final String RIGHT_REQUEST = "M10.RIGHT_CHANGE_REQUEST";

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    RetentionDatabase database;

    @Autowired
    SharedRowRetention sharedRows;

    @Autowired
    List<RetentionProvider> retention;

    private RetentionProvider provider(String type) {
        return retention.stream()
                .filter(p -> type.equals(p.dataType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có RetentionProvider cho " + type));
    }

    private long simpleRecord(String kind, long residentId) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO simple_record (kind, module, resident_id, entity_type, entity_id, status, content,"
                                    + " created_at, created_by, version) VALUES (?, 'M10', ?, 'M01.RESIDENT', ?, 'ENDED',"
                                    + " '{}', UTC_TIMESTAMP(6), 1, 0)",
                            new String[] {"id"});
                    ps.setString(1, kind);
                    ps.setLong(2, residentId);
                    ps.setLong(3, residentId);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }

    @ParameterizedTest
    @ValueSource(strings = {STAY, RIGHT_REQUEST})
    void parentKind_hasRetentionProvider_perResidentRecord(String type) {
        assertThat(provider(type).retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        assertThat(provider(type).referencesTo(1L)).isEmpty();
    }

    @Test
    void stay_deletedInChain_removesExtensionsAndNightMarkers_ofThatStayOnly() {
        long resident = TestUsers.nextId();
        long gone = simpleRecord(STAY, resident);
        long kept = simpleRecord(STAY, resident);
        for (long stay : List.of(gone, kept)) {
            jdbc.update(
                    "INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at)"
                            + " VALUES (?, ?, 'M10', 'M10.FAMILY_STAY_EXTENSION', '{}', FALSE, UTC_TIMESTAMP(6))",
                    STAY,
                    stay);
            jdbc.update(
                    "INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at)"
                            + " VALUES ('M10.STAY_NIGHT', ?, ?, '2026-11-01', UTC_TIMESTAMP(6))",
                    STAY,
                    stay);
        }

        // Thứ tự của job (ExpiredDataPurge): dòng bảng chung của bản ghi, rồi bản ghi
        database.transaction().executeWithoutResult(s -> {
            sharedRows.deleteFor(List.of(new EntityRef(STAY, gone)));
            provider(STAY).delete(List.of(gone));
        });

        assertThat(count("SELECT COUNT(*) FROM simple_record WHERE kind = ? AND id = ?", STAY, gone))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM record_event WHERE entity_type = ? AND entity_id = ?", STAY, gone))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM reminder_marker WHERE entity_type = ? AND entity_id = ?", STAY, gone))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM simple_record WHERE kind = ? AND id = ?", STAY, kept))
                .isOne();
        assertThat(count("SELECT COUNT(*) FROM record_event WHERE entity_type = ? AND entity_id = ?", STAY, kept))
                .isOne();
        assertThat(count("SELECT COUNT(*) FROM reminder_marker WHERE entity_type = ? AND entity_id = ?", STAY, kept))
                .isOne();
    }

    @Test
    void rightRequest_deletedInChain_removesReminderMarkers() {
        long resident = TestUsers.nextId();
        long request = simpleRecord(RIGHT_REQUEST, resident);
        jdbc.update(
                "INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at)"
                        + " VALUES ('M10.RIGHT_CHANGE_REMINDER', ?, ?, 'REMIND', UTC_TIMESTAMP(6))",
                RIGHT_REQUEST,
                request);

        database.transaction().executeWithoutResult(s -> {
            sharedRows.deleteFor(List.of(new EntityRef(RIGHT_REQUEST, request)));
            provider(RIGHT_REQUEST).delete(List.of(request));
        });

        assertThat(count("SELECT COUNT(*) FROM simple_record WHERE id = ?", request))
                .isZero();
        assertThat(count(
                        "SELECT COUNT(*) FROM reminder_marker WHERE entity_type = ? AND entity_id = ?",
                        RIGHT_REQUEST,
                        request))
                .isZero();
    }

    @Test
    void recordsOfAResidentStillStaying_areNotCandidates() {
        long resident = TestUsers.nextId();
        long stay = simpleRecord(STAY, resident);

        assertThat(provider(STAY).expiredCandidates(time.now().plus(Duration.ofDays(1)), 1000))
                .extracting(RetentionProvider.Candidate::id)
                .doesNotContain(stay);
    }
}
