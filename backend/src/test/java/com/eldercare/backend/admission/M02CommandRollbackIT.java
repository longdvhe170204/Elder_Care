package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.resident.internal.status.service.ResidentStatusService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 004 T070 (research T-03; 001 FR-047, H-03): module chạy sau M02 lỗi thì cả lệnh rollback, không để lại tác động của
 * M02. Mỗi lệnh chạy hai lần trên cùng dữ liệu: lần lỗi phải giữ nguyên các bảng của M02, lần thành công phải đổi chúng,
 * nên lần lỗi không thể đạt chỉ vì M02 chưa làm gì.
 */
@AutoConfigureMockMvc
@WithMockUser
class M02CommandRollbackIT extends DeathTestBase {

    private static final StatusCommand[] ALL = {
        StatusCommand.COMPLETE_ADMISSION,
        StatusCommand.CANCEL_ADMISSION,
        StatusCommand.GRANT_TEMPORARY_ABSENCE,
        StatusCommand.TRANSFER_OUT,
        StatusCommand.RECORD_RETURN,
        StatusCommand.END_RESIDENCY,
        StatusCommand.RECORD_DEATH
    };

    @Autowired
    List<StatusTransitionParticipant> participants;

    @BeforeEach
    void allCommands() {
        otherModules.interestedIn(ALL);
    }

    /** Toàn bộ dòng của người này ở các bảng M02 ghi trong tác động của lệnh. */
    private String m02State(long residentId) {
        List<Object> rows = new ArrayList<>();
        for (String table :
                List.of("resident_module_state", "admission_record", "contract", "absence", "residency_ending")) {
            rows.add(jdbc.queryForList("SELECT * FROM " + table + " WHERE resident_id = ? ORDER BY 1", residentId));
        }
        return rows.toString();
    }

    private void rollsBackThenApplies(long residentId, ResidentStatus before, ResidentStatus after, Runnable command) {
        String state = m02State(residentId);
        otherModules.failOnApply(new IllegalStateException("Module sau M02 lỗi"));

        assertThatThrownBy(command::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Module sau M02 lỗi");

        assertThat(m02State(residentId)).as("lệnh lỗi không để lại tác động").isEqualTo(state);
        assertThat(currentStatus(residentId)).isEqualTo(before);

        otherModules.failOnApply(null);
        command.run();

        assertThat(m02State(residentId))
                .as("lệnh thành công có tác động của M02")
                .isNotEqualTo(state);
        assertThat(currentStatus(residentId)).isEqualTo(after);
    }

    @Test
    void H_03_m02RunsBeforeTheFailingModule() {
        StatusTransitionParticipant m02 = participants.stream()
                .filter(p -> p.getClass().getSimpleName().equals("M02StatusParticipant"))
                .findFirst()
                .orElseThrow();

        assertThat(participants.indexOf(m02)).isLessThan(participants.indexOf(otherModules));
    }

    @Test
    void FR_047_completeAdmission() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, com.eldercare.backend.resident.api.dto.CareLevel.BASIC);
        activeContract(residentId, body(residentId));
        user.set(office.user());

        rollsBackThenApplies(
                residentId,
                ResidentStatus.ADMISSION_IN_PROGRESS,
                ResidentStatus.IN_RESIDENCE,
                () -> statusService.completeAdmission(
                        ResidentStatusService.target(residentId),
                        new Reason("Hoàn tất thử"),
                        JsonNodeFactory.instance.objectNode()));
    }

    @Test
    void FR_047_cancelAdmission() throws Exception {
        long residentId = residentWithRepresentative();
        activeContract(residentId, body(residentId));
        user.set(office.user());

        rollsBackThenApplies(
                residentId,
                ResidentStatus.ADMISSION_IN_PROGRESS,
                ResidentStatus.ADMISSION_CANCELLED,
                () -> statusService.cancelAdmission(
                        ResidentStatusService.target(residentId),
                        new Reason("Gia đình rút hồ sơ"),
                        JsonNodeFactory.instance.objectNode()));
    }

    @Test
    void FR_047_grantTemporaryAbsence() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        Instant expectedReturn = at(d0.plusDays(3), 10, 0);

        rollsBackThenApplies(
                residentId,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.TEMPORARILY_ABSENT,
                () -> grant(residentId, "HOME_VISIT", expectedReturn));
    }

    @Test
    void FR_047_transferOut() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);

        rollsBackThenApplies(
                residentId, ResidentStatus.IN_RESIDENCE, ResidentStatus.HOSPITALIZED, () -> transferOut(residentId));
    }

    @Test
    void FR_047_recordReturn() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        grant(residentId, "HOME_VISIT", at(d0.plusDays(3), 10, 0));
        clockAt(d0.plusDays(1), 10, 0);

        rollsBackThenApplies(
                residentId,
                ResidentStatus.TEMPORARILY_ABSENT,
                ResidentStatus.IN_RESIDENCE,
                () -> recordReturn(residentId, null));
    }

    @Test
    void FR_047_endResidency() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(3));
        Instant effectiveAt = time.now();

        rollsBackThenApplies(
                ready.residentId(),
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.RESIDENCY_ENDED,
                () -> endResidency(ready.residentId(), ready.endingId(), effectiveAt));
    }

    @Test
    void FR_047_recordDeath() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 9, 0);
        Instant deathAt = time.now();

        rollsBackThenApplies(
                residentId,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.DECEASED,
                () -> recordDeath(doctor, residentId, deathDetails(deathAt, "Suy tim", null)));
    }
}
