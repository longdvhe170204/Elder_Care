package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * 001 T045 (FR-047b, Q-342, US4 #10–11): "Ghi nhận qua đời" do Bác sĩ thực hiện khi Đang lưu trú (mất tại viện). Khi
 * Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện (mất ngoài viện), Bác sĩ hoặc Hành chính được làm; Hành chính
 * bắt buộc kèm {@code evidenceFileId}.
 */
class RecordDeathIT extends StatusTestBase {

    private static final long EVIDENCE_FILE = 4242L;

    private Map<String, Object> lastHistory(long residentId) {
        return jdbc.queryForMap(
                "SELECT command, reason, basis_type, basis_id FROM resident_status_history"
                        + " WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                residentId);
    }

    @Test
    void FR_047b_doctorAtTheFacility_recordsDeath() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(doctor.user());

        var outcome = statusService.recordDeath(target(id), reason(), noDetails());

        assertThat(outcome.status()).isEqualTo(ResidentStatus.DECEASED);
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.DECEASED);
        assertThat(lastHistory(id).get("command")).isEqualTo("RECORD_DEATH");
    }

    @Test
    void FR_047b_officeStaffAtTheFacility_isRefused() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        BusinessException e = failure(
                () -> statusService.recordDeath(target(id), reason(), details("evidenceFileId", EVIDENCE_FILE)));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(e.errorCode()).isEqualTo("DEATH_AT_FACILITY_NEEDS_DOCTOR");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_047b_officeStaffOutsideTheFacility_needsEvidence() {
        for (ResidentStatus outside : new ResidentStatus[] {
            ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.OFF_SITE_ACTIVITY, ResidentStatus.HOSPITALIZED
        }) {
            long missing = residentIn(outside);
            long withEvidence = residentIn(outside);
            user.set(office.user());

            BusinessException e = failure(() -> statusService.recordDeath(target(missing), reason(), noDetails()));
            assertThat(e).as("%s không bằng chứng", outside).isNotNull();
            assertThat(e.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(e.errorCode()).isEqualTo("EVIDENCE_REQUIRED");
            assertThat(currentStatus(missing)).isEqualTo(outside);

            statusService.recordDeath(target(withEvidence), reason(), details("evidenceFileId", EVIDENCE_FILE));
            assertThat(currentStatus(withEvidence)).isEqualTo(ResidentStatus.DECEASED);
            Map<String, Object> row = lastHistory(withEvidence);
            assertThat(row.get("basis_type")).isEqualTo("EVIDENCE_FILE");
            assertThat(((Number) row.get("basis_id")).longValue()).isEqualTo(EVIDENCE_FILE);
        }
    }

    @Test
    void FR_047b_doctorOutsideTheFacility_needsNoEvidence() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        user.set(doctor.user());

        statusService.recordDeath(target(id), reason(), noDetails());

        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.DECEASED);
    }

    @Test
    void FR_047b_nurseCannotRecordDeath() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        assignNurse(nurse, id);
        user.set(nurse.user());

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> statusService.recordDeath(target(id), reason(), noDetails()))
                .isInstanceOf(com.cambae.backend.common.error.PermissionDeniedException.class);
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_046_everyUserCommandNeedsAReason() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(doctor.user());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        statusService.recordDeath(target(id), new com.cambae.backend.common.Reason(" "), noDetails()))
                .isInstanceOf(com.cambae.backend.common.error.ReasonRequiredException.class);
    }
}
