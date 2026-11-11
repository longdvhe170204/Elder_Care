package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.spi.TripIncidentPort;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentProgressionService;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 007 T063 (014 FR-046 → 048, BR-M04-17): bean thật của {@code care.api.spi.TripIncidentPort} do M05 cài ghi sự cố "Đi
 * lạc hoặc không trở về", Khẩn cấp, nguồn hoạt động ngoài viện; tìm thấy thì ghi diễn biến Đã tìm thấy, không tự đóng.
 */
@Import(SwitchableUserConfig.class)
class T063_TripIncidentPortIT extends IncidentHttpTestBase {

    @Autowired
    TripIncidentPort port;

    @Autowired
    SwitchableUser user;

    @Autowired
    IncidentProgressionStore progressionStore;

    @Autowired
    IncidentProgressionService progressions;

    @Test
    void T063_missingOnTrip_recordsCriticalOffsiteIncident_foundAddsProgressionWithoutClosing() {
        user.set(nurse.user());
        long trip = TestUsers.nextId();

        long id = port.recordMissing(
                resident,
                trip,
                nurse.staffId(),
                "Cổng công viên",
                time.now().minusSeconds(600),
                "Không thấy khi điểm danh về");

        var row = jdbc.queryForMap(
                "SELECT type_code, context, level, location, resident_id, status FROM incident WHERE id=?", id);
        assertThat(row.get("type_code")).isEqualTo("MISSING_OR_NOT_RETURNED");
        assertThat(row.get("context")).isEqualTo("OFFSITE_ACTIVITY");
        assertThat(row.get("level")).isEqualTo("CRITICAL");
        assertThat(row.get("location")).isEqualTo("Cổng công viên");
        assertThat(((Number) row.get("resident_id")).longValue()).isEqualTo(resident);

        port.recordFoundReturned(id, resident, trip, time.now());

        assertThat(progressionStore.typeIds(id)).contains(progressions.typeIdByCode("FOUND"));
        assertThat(jdbc.queryForObject("SELECT status FROM incident WHERE id=?", String.class, id))
                .isNotIn("CLOSED", "CANCELLED");
    }
}
