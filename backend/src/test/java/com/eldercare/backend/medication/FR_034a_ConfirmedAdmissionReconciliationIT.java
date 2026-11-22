package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.eldercare.backend.medication.internal.status.service.MedicationStatusParticipant;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * FR-034a, Q-57 (BF-01 bước 12 → 14): phiếu "tiếp nhận" đã Đã xác nhận trước lệnh Hoàn tất tiếp nhận thì lệnh chạy
 * được và phiếu đã khóa không bị ghi lại (trigger {@code trg_medication_reconciliation_row_lock} chặn mọi UPDATE).
 * Phiếu thêm bằng SQL để chỉ lệnh Hoàn tất tiếp nhận đi qua JPA.
 */
class FR_034a_ConfirmedAdmissionReconciliationIT extends M07IntegrationTestBase {

    private static final String LINES = "[{\"id\": null, \"reconciliationId\": null, \"itemOrder\": 1,"
            + " \"source\": \"ADMISSION_CURRENT_MEDICATION\", \"prescriptionId\": null,"
            + " \"admissionMedicationItemId\": null, \"medicationDescription\": \"Amlodipin 5mg, 1 viên sáng\","
            + " \"decision\": \"CONTINUE\", \"reason\": null, \"newPrescriptionJson\": null, \"createdById\": 10,"
            + " \"createdAt\": \"2026-11-20T09:00:00.000000Z\", \"discontinuedOutsideTicket\": false,"
            + " \"createdPrescriptionId\": null}]";

    @Autowired
    private MedicationStatusParticipant participant;

    @Autowired
    private TransactionTemplate tx;

    private final long residentId = TestUsers.nextId();

    @Test
    @DisplayName("FR-034a: Hoàn tất tiếp nhận khi đã có phiếu tiếp nhận Đã xác nhận thành công, phiếu không bị ghi lại")
    void completeAdmissionWithConfirmedAdmissionTicketDoesNotRewriteTheLockedTicket() {
        jdbc.update(
                "INSERT INTO medication_reconciliation (resident_id, reason, status, created_by_id, created_at,"
                        + " submitted_by_id, submitted_at, confirmed_by_id, confirmed_at, locked_at, `lines`, version)"
                        + " VALUES (?, 'ADMISSION', 'CONFIRMED', 10, UTC_TIMESTAMP(6), 10, UTC_TIMESTAMP(6), 11,"
                        + " UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), CAST(? AS JSON), 0)",
                residentId,
                LINES);

        TransitionResult result = new TransitionResult(
                residentId,
                ResidentStatus.ADMISSION_IN_PROGRESS,
                ResidentStatus.IN_RESIDENCE,
                StatusCommand.COMPLETE_ADMISSION,
                1L,
                Instant.now(),
                JsonMapper.builder().build().readTree("{}"));

        assertThatCode(() -> tx.executeWithoutResult(s -> participant.applyEffects(result)))
                .doesNotThrowAnyException();
        assertThat(jdbc.queryForObject(
                        "SELECT version FROM medication_reconciliation WHERE resident_id = ?",
                        Integer.class,
                        residentId))
                .isZero();
    }
}
