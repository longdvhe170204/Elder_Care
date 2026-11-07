package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.spi.PostDeathItemPort;
import com.eldercare.backend.incident.internal.governance.service.IncidentStatusParticipant;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bean tham gia lệnh trạng thái của {@code incident} (T060; research S-17; quickstart Q57): điều kiện
 * {@code END_D_ALERTS_INCIDENTS} miễn được cho Kết thúc lưu trú, tác động trạng thái cuối (dừng leo thang cảnh báo và sự
 * cố, khoảng theo dõi sau ngã Đã hết) và mục việc sau qua đời {@code ALERTS_INCIDENTS}.
 */
class FR_038_FinalStatusIT extends IncidentHttpTestBase {
    @Autowired
    IncidentStatusParticipant participant;

    @Autowired
    List<PostDeathItemPort> postDeathItems;

    @Autowired
    FallMonitoringStore windows;

    @Test
    void Q57_endResidencyBlockedByOpenAlert_waivable() {
        assertThat(participant.checkConditions(context(StatusCommand.END_RESIDENCY)))
                .isEmpty();

        alert("RECEIVED");

        var conditions = participant.checkConditions(context(StatusCommand.END_RESIDENCY));
        assertThat(conditions).singleElement().satisfies(condition -> {
            assertThat(condition.code()).isEqualTo("END_D_ALERTS_INCIDENTS");
            assertThat(condition.waivable()).isTrue();
        });
        assertThat(participant.checkConditions(context(StatusCommand.RECORD_DEATH)))
                .isEmpty();
    }

    private long alert(String status) {
        long id = TestUsers.nextId();
        var at = Timestamp.from(time.now().minusSeconds(600));
        jdbc.update("""
                INSERT INTO alert (id, resident_id, floor_area_id, type, type_lock, level, first_at, latest_at, status,
                  escalation_level, version)
                VALUES (?, ?, ?, 'CHI_SO', ?, 'MEDIUM', ?, ?, ?, 0, 0)
                """, id, resident, floor, "VUOT_NGUONG:" + id, at, at, status);
        return id;
    }

    private CommandContext context(StatusCommand command) {
        return new CommandContext(resident, ResidentStatus.IN_RESIDENCE, command, nurse.user(), time.now(), null);
    }

    private TransitionResult result(StatusCommand command, ResidentStatus to) {
        return new TransitionResult(resident, ResidentStatus.IN_RESIDENCE, to, command, 1L, time.now(), null);
    }
}
