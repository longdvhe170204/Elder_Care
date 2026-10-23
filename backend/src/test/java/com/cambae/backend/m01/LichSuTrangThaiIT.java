package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.Condition;
import com.cambae.backend.m01.api.dto.ConditionWaiver;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 001 T045 (FR-046, FR-047; US4 #9–10): lịch sử trạng thái ghi đủ trường và chỉ thêm; yêu cầu ngoại lệ đã duyệt của 004
 * miễn điều kiện miễn được và ghi {@code condition_waiver}, điều kiện không miễn được vẫn chặn.
 */
class LichSuTrangThaiIT extends StatusTestBase {

    private static final Condition WAIVABLE = new Condition("UNPAID_FEES", "Còn nợ", "BR-M11-22", true);
    private static final Condition HARD = new Condition("BELONGINGS_NOT_RETURNED", "Còn đồ gửi", "BR-M12-03", false);

    @Test
    void FR_046_historyRecordsFromToCommandActorReasonAndTime() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        var at = time.now();

        var outcome = statusService.grantTemporaryAbsence(
                target(id), new com.cambae.backend.common.Reason("Đi khám ngoài"), noDetails());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT from_status, to_status, command, performed_by, by_system, reason"
                        + " FROM resident_status_history WHERE id = ?",
                outcome.historyId());
        assertThat(row.get("from_status")).isEqualTo("IN_RESIDENCE");
        assertThat(row.get("to_status")).isEqualTo("TEMPORARILY_ABSENT");
        assertThat(row.get("command")).isEqualTo("GRANT_TEMPORARY_ABSENCE");
        assertThat(((Number) row.get("performed_by")).longValue()).isEqualTo(office.accountId());
        assertThat(row.get("by_system")).isIn(false, 0);
        assertThat(row.get("reason")).isEqualTo("Đi khám ngoài");
        assertThat(outcome.history().at()).isAfterOrEqualTo(at).isBeforeOrEqualTo(time.now());
    }

    @Test
    void FR_046_historyEndpointListsEntriesOldestFirstWithActorName() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        statusService.grantTemporaryAbsence(target(id), reason(), noDetails());
        statusService.recordReturn(target(id), reason(), noDetails());

        var history = statusService.history(target(id));

        assertThat(history)
                .extracting(e -> e.command())
                .containsSubsequence("CREATE_PROFILE", "SEED", "GRANT_TEMPORARY_ABSENCE", "RECORD_RETURN");
        assertThat(history.getFirst().from()).isNull();
        assertThat(history.getLast().to()).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(history.getLast().reason()).isEqualTo("Lý do thử");
    }

    @Test
    void FR_009_historyIsAppendOnly_appUserCannotUpdateOrDelete() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> jdbc.update("UPDATE resident_status_history SET reason = 'x' WHERE resident_id = ?", id))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> jdbc.update("DELETE FROM resident_status_history WHERE resident_id = ?", id))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void FR_047_approvedExceptionWaivesWaivableConditions_andIsRecordedInTheHistory() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        one.unmet(WAIVABLE);
        two.waive(new ConditionWaiver(501L, Set.of("UNPAID_FEES")));
        user.set(office.user());

        var outcome = statusService.endResidency(target(id), reason(), noDetails());

        assertThat(outcome.status()).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
        String waiver = jdbc.queryForObject(
                "SELECT condition_waiver FROM resident_status_history WHERE id = ?", String.class, outcome.historyId());
        assertThat(waiver).contains("501").contains("UNPAID_FEES");
        assertThat(one.applied()).hasSize(1);
    }

    @Test
    void FR_047_waiverNeverCoversAConditionThatIsNotWaivable_orNotListedInIt() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        one.unmet(WAIVABLE).unmet(HARD);
        two.waive(new ConditionWaiver(501L, Set.of("UNPAID_FEES", "BELONGINGS_NOT_RETURNED")));
        user.set(office.user());

        BusinessException e = failure(() -> statusService.endResidency(target(id), reason(), noDetails()));

        assertThat(e).isNotNull();
        assertThat(e.errorCode()).isEqualTo("UNMET_CONDITIONS");
        assertThat(unmetCodes(e)).containsExactly("BELONGINGS_NOT_RETURNED");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_047_conditionNotNamedInTheWaiverStillBlocks() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        one.unmet(WAIVABLE);
        two.waive(new ConditionWaiver(501L, Set.of("SOMETHING_ELSE")));
        user.set(office.user());

        BusinessException e = failure(() -> statusService.endResidency(target(id), reason(), noDetails()));

        assertThat(unmetCodes(e)).containsExactly("UNPAID_FEES");
    }

    @Test
    void FR_044_aPairOutsideTheTableIsRejected_withoutAskingParticipants() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        BusinessException e = failure(() -> statusService.recordReturn(target(id), reason(), noDetails()));

        assertThat(e).isNotNull();
        assertThat(e.errorCode()).isEqualTo("INVALID_TRANSITION");
        assertThat(one.applied()).isEmpty();
    }
}
