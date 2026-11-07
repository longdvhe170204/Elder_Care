package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.spi.PostDeathItemPort;
import com.eldercare.backend.incident.internal.governance.service.IncidentStatusParticipant;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.resident.api.dto.CommandContext;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

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
    void T060_orderAfterHealth() {
        assertThat(participant).isInstanceOf(StatusTransitionParticipant.class);
        assertThat(IncidentStatusParticipant.class.getAnnotation(Order.class).value())
                .isEqualTo(51);
        assertThat(participant.interestedCommands())
                .containsExactlyInAnyOrder(
                        StatusCommand.END_RESIDENCY, StatusCommand.RECORD_DEATH, StatusCommand.CANCEL_ADMISSION);
    }

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

    @Test
    void Q57_openIncidentBlocks_closedDoesNot() {
        long monitoring = incident("FACILITY", "LOW", "MONITORING", nurse.accountId(), nurse.accountId());
        assertThat(participant.checkConditions(context(StatusCommand.END_RESIDENCY)))
                .extracting(c -> c.code())
                .containsExactly("END_D_ALERTS_INCIDENTS");

        jdbc.update("UPDATE incident SET status = 'CLOSED' WHERE id = ?", monitoring);
        assertThat(participant.checkConditions(context(StatusCommand.END_RESIDENCY)))
                .isEmpty();
    }

    @Test
    void FR_038_deathStopsEscalation_andExpiresFallWindow() {
        long alert = alert("ESCALATED");
        long incident = incident("FACILITY", "MEDIUM", "NEW", null, nurse.accountId());
        long window = windows.start(
                resident,
                incident,
                time.now(),
                time.now().plus(Duration.ofHours(72)),
                Duration.ofHours(4),
                nurse.accountId());

        participant.applyEffects(result(StatusCommand.RECORD_DEATH, ResidentStatus.DECEASED));

        assertThat(jdbc.queryForObject("SELECT escalation_stopped FROM alert WHERE id = ?", Boolean.class, alert))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT status FROM alert WHERE id = ?", String.class, alert))
                .isEqualTo("ESCALATED");
        assertThat(jdbc.queryForObject("SELECT escalation_stopped FROM incident WHERE id = ?", Boolean.class, incident))
                .isTrue();
        assertThat(incidentStatus(incident)).isEqualTo("NEW");
        assertThat(jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, window))
                .isEqualTo(FallMonitoringStore.EXPIRED);
    }

    @Test
    void S17_postDeathItem_notApplicable_notCompleted_completed() {
        var item = postDeathItems.stream()
                .filter(port -> port.code() == PostDeathItemCode.ALERTS_INCIDENTS)
                .filter(port -> port.getClass().getName().startsWith("com.eldercare.backend.incident"))
                .findFirst()
                .orElseThrow();
        assertThat(item.status(resident).status()).isEqualTo(PostDeathItemStatus.NOT_APPLICABLE);

        long alert = alert("IN_PROGRESS");
        assertThat(item.status(resident).status()).isEqualTo(PostDeathItemStatus.NOT_COMPLETED);

        jdbc.update("UPDATE alert SET status = 'CLOSED' WHERE id = ?", alert);
        assertThat(item.status(resident).status()).isEqualTo(PostDeathItemStatus.COMPLETED);
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
