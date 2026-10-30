package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 001 T045 (BR-M01-06, FR-046; research H-03): lệnh do module khác gọi qua {@code ResidentStatusApi}. Điểm danh của 014
 * ghi {@code by_system}, không cần lý do, người thực hiện là người điểm danh; 007, 004 gọi lệnh có lý do; lệnh ngoài
 * danh sách của từng module bị từ chối. Mọi kiểm tra của H-02 vẫn chạy.
 */
class BR_M01_06_ModuleCommandsIT extends StatusTestBase {

    private Map<String, Object> lastHistory(long residentId) {
        return jdbc.queryForMap(
                "SELECT from_status, to_status, command, performed_by, by_system, reason, basis_type, basis_id"
                        + " FROM resident_status_history WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                residentId);
    }

    @Test
    void BR_M01_06_rollCallDeparture_isBySystem_noReasonNeeded_performedByTheRollCaller() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);

        var result = inTransaction(() -> statusApi.execute(
                id, StatusCommand.ROLL_CALL_DEPARTURE, nurse.user(), Optional.empty(), noDetails(), sourceRecord(77L)));

        assertThat(result.to()).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        Map<String, Object> row = lastHistory(id);
        assertThat(row.get("command")).isEqualTo("ROLL_CALL_DEPARTURE");
        assertThat(row.get("by_system")).isIn(true, 1);
        assertThat(row.get("reason")).isNull();
        assertThat(((Number) row.get("performed_by")).longValue()).isEqualTo(nurse.accountId());
        assertThat(row.get("basis_type")).isEqualTo("SOURCE_RECORD");
        assertThat(((Number) row.get("basis_id")).longValue()).isEqualTo(77L);
    }

    @Test
    void BR_M01_06_rollCallReturn_andCancelDepartureRecord_bringTheResidentBack() {
        long returned = residentIn(ResidentStatus.OFF_SITE_ACTIVITY);
        long cancelled = residentIn(ResidentStatus.OFF_SITE_ACTIVITY);

        inTransaction(() -> statusApi.execute(
                returned,
                StatusCommand.ROLL_CALL_RETURN,
                nurse.user(),
                Optional.empty(),
                noDetails(),
                sourceRecord(5L)));
        inTransaction(() -> statusApi.execute(
                cancelled,
                StatusCommand.CANCEL_DEPARTURE_RECORD,
                office.user(),
                Optional.empty(),
                noDetails(),
                sourceRecord(5L)));

        assertThat(currentStatus(returned)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(currentStatus(cancelled)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(lastHistory(cancelled).get("command")).isEqualTo("CANCEL_DEPARTURE_RECORD");
    }

    @Test
    void BR_M01_06_transferOutFromAnIncident_needsAReason_andRecordsTheIncident() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);

        BusinessException noReason = failure(() -> inTransaction(() -> statusApi.execute(
                id, StatusCommand.TRANSFER_OUT, doctor.user(), Optional.empty(), noDetails(), sourceRecord(9L))));
        assertThat(noReason).isNotNull();
        assertThat(noReason.errorCode()).isEqualTo("REASON_REQUIRED");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);

        inTransaction(() -> statusApi.execute(
                id,
                StatusCommand.TRANSFER_OUT,
                doctor.user(),
                Optional.of(new Reason("Cấp cứu")),
                noDetails(),
                sourceRecord(9L)));

        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.HOSPITALIZED);
        Map<String, Object> row = lastHistory(id);
        assertThat(row.get("by_system")).isIn(false, 0);
        assertThat(row.get("reason")).isEqualTo("Cấp cứu");
        assertThat(row.get("basis_type")).isEqualTo("SOURCE_RECORD");
    }

    @Test
    void BR_M01_06_cancelAdmissionFrom004_works_andCommandsOutsideTheListAreRefused() {
        long pending = admissionResident();
        long living = residentIn(ResidentStatus.IN_RESIDENCE);

        inTransaction(() -> statusApi.execute(
                pending,
                StatusCommand.CANCEL_ADMISSION,
                office.user(),
                Optional.of(new Reason("Hồ sơ chờ bị từ chối")),
                noDetails(),
                sourceRecord(3L)));
        assertThat(currentStatus(pending)).isEqualTo(ResidentStatus.ADMISSION_CANCELLED);

        BusinessException refused = failure(() -> inTransaction(() -> statusApi.execute(
                living,
                StatusCommand.END_RESIDENCY,
                office.user(),
                Optional.of(new Reason("Không được gọi từ module khác")),
                noDetails(),
                sourceRecord(3L))));
        assertThat(refused).isNotNull();
        assertThat(refused.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(refused.errorCode()).isEqualTo("COMMAND_NOT_ALLOWED_FOR_MODULE");
        assertThat(currentStatus(living)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void BR_M01_06_moduleCommandStillRunsTheTransitionTableAndConditions() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        one.unmet(new com.eldercare.backend.resident.api.dto.Condition("X", "chặn", "BR-M01-04", false));

        BusinessException wrongState = failure(() -> inTransaction(() -> statusApi.execute(
                id, StatusCommand.ROLL_CALL_DEPARTURE, nurse.user(), Optional.empty(), noDetails(), sourceRecord(1L))));
        assertThat(wrongState.errorCode()).isEqualTo("INVALID_TRANSITION");

        long living = residentIn(ResidentStatus.IN_RESIDENCE);
        BusinessException unmet = failure(() -> inTransaction(() -> statusApi.execute(
                living,
                StatusCommand.ROLL_CALL_DEPARTURE,
                nurse.user(),
                Optional.empty(),
                noDetails(),
                sourceRecord(1L))));
        assertThat(unmet.errorCode()).isEqualTo("UNMET_CONDITIONS");
        assertThat(unmetCodes(unmet)).containsExactly("X");
    }

    @Test
    void BR_M01_06_previewConditionsForModule004_listsWithoutChange() {
        long id = admissionResident();
        one.unmet(new com.eldercare.backend.resident.api.dto.Condition("DEPOSIT", "Chưa đặt cọc", "BR-M02-22", false));

        var conditions = statusApi.previewConditions(id, StatusCommand.COMPLETE_ADMISSION);

        assertThat(conditions).extracting(c -> c.code()).contains("DEPOSIT");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
    }
}
