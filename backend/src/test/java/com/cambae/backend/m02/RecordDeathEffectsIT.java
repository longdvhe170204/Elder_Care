package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import com.cambae.backend.m02.api.events.AbsenceClosed;
import com.cambae.backend.m02.api.events.DeathRecorded;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T062 (US8; FR-069, FR-070, FR-071, BR-M02-21): phần của 004 trong lệnh Ghi nhận qua đời của 001. Bản ghi qua đời được
 * lập, hợp đồng Chấm dứt "Qua đời", lượt vắng đóng tại thời điểm qua đời, hồ sơ kết thúc đang chuẩn bị chuyển Đã hủy, năm mục
 * việc sau qua đời được tạo; lệnh không bị chặn bởi đồ gửi, thuốc gửi, chi phí hay sự cố; Hành chính ghi nhận phải có bằng
 * chứng (001 kiểm lúc nhận lệnh).
 */
@AutoConfigureMockMvc
@WithMockUser
class RecordDeathEffectsIT extends DeathTestBase {

    private Map<String, Object> deathRecord(long residentId) {
        return jdbc.queryForMap("SELECT * FROM death_record WHERE resident_id = ?", residentId);
    }

    private String taskStatus(long recordId, String code) {
        return jdbc.queryForObject(
                "SELECT status FROM post_death_task WHERE death_record_id = ? AND item_code = ?",
                String.class,
                recordId,
                code);
    }

    @Test
    void FR_070_doctorRecordsDeath_writesTheRecord_terminatesTheContract_andCancelsThePreparingEnding()
            throws Exception {
        long residentId = inResidence();
        long endingId = createEnding(residentId, d0.plusDays(10));
        long contractId = jdbc.queryForObject("SELECT id FROM contract WHERE resident_id = ?", Long.class, residentId);
        clockAt(d0, 9, 0);
        Instant deathAt = time.now();
        item(PostDeathItemCode.BELONGINGS).status(residentId, PostDeathItemStatus.NOT_COMPLETED);
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.COMPLETED);

        recordDeath(doctor, residentId, deathDetails(deathAt, "Suy tim", null));

        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.DECEASED);
        Map<String, Object> record = deathRecord(residentId);
        assertThat(record.get("place")).isEqualTo("Phòng P201");
        assertThat(record.get("discovered_by")).isEqualTo("Nhân viên chăm sóc trực");
        assertThat(((Number) record.get("confirmed_by_id")).longValue()).isEqualTo(doctor.staffId());
        assertThat(record.get("cause_of_death")).isEqualTo("Suy tim");
        assertThat(((Number) record.get("recorded_by")).longValue()).isEqualTo(doctor.accountId());
        assertThat(contractStatus(contractId)).isEqualTo("TERMINATED");
        assertThat(jdbc.queryForObject(
                        "SELECT termination_reason FROM contract WHERE id = ?", String.class, contractId))
                .contains("Qua đời");
        assertThat(endingStatus(endingId)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT cancel_reason FROM residency_ending WHERE id = ?", String.class, endingId))
                .isEqualTo("Qua đời");
        assertThat(calls())
                .as("mốc dừng tính phí là ngày qua đời nên không gọi hủy mốc dừng")
                .containsExactly("createDraft:" + residentId + ":" + d0.plusDays(10));
        long recordId = ((Number) record.get("id")).longValue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM post_death_task WHERE death_record_id = ?", Integer.class, recordId))
                .isEqualTo(5);
        assertThat(taskStatus(recordId, "BELONGINGS")).isEqualTo("NOT_COMPLETED");
        assertThat(taskStatus(recordId, "SETTLEMENT")).isEqualTo("COMPLETED");
        assertThat(taskStatus(recordId, "STORED_MEDICATION")).isEqualTo("NOT_APPLICABLE");
        assertThat(residencyClosed(residentId)).as("còn mục chưa xong").isFalse();
        assertThat(events.stream(DeathRecorded.class).filter(e -> e.residentId() == residentId))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.at()).isEqualTo(deathAt);
                    assertThat(e.recordId()).isEqualTo(recordId);
                });
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_DEATH_RECORDED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(recordId)))
                .isEqualTo(1);
    }

    @Test
    void FR_070_deathDuringAnAbsence_closesTheAbsenceAtTheMomentOfDeath() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(9), 10, 0));
        clockAt(d0.plusDays(2), 3, 20);
        Instant deathAt = time.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);

        recordDeath(doctor, residentId, deathDetails(deathAt, null, null));

        assertThat(jdbc.queryForObject("SELECT status FROM absence WHERE id = ?", String.class, absenceId))
                .isEqualTo("CLOSED");
        assertThat(jdbc.queryForObject(
                                "SELECT returned_at FROM absence WHERE id = ?", java.sql.Timestamp.class, absenceId)
                        .toInstant())
                .isEqualTo(deathAt);
        assertThat(events.stream(AbsenceClosed.class).filter(e -> e.absenceId() == absenceId))
                .hasSize(1);
        assertThat(deathRecord(residentId).get("cause_of_death"))
                .as("nguyên nhân tùy chọn")
                .isNull();
    }

    @Test
    void FR_070_openItemsDoNotBlockTheCommand() throws Exception {
        long residentId = inResidence();
        allItems(residentId, PostDeathItemStatus.NOT_COMPLETED);

        doctorRecordsDeath(residentId, null);

        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.DECEASED);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM post_death_task WHERE status = 'NOT_COMPLETED' AND death_record_id ="
                                + " (SELECT id FROM death_record WHERE resident_id = ?)",
                        Integer.class,
                        residentId))
                .isEqualTo(5);
        assertThat(residencyClosed(residentId)).isFalse();
    }

    @Test
    void FR_072_whenEveryItemIsAlreadyNotApplicableOrDone_theResidencyIsClosedAtOnce() throws Exception {
        long residentId = inResidence();
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.COMPLETED);

        doctorRecordsDeath(residentId, null);

        assertThat(residencyClosed(residentId)).isTrue();
    }

    @Test
    void FR_069_administrativeStaffOutsideTheFacility_mustAttachEvidence() throws Exception {
        long residentId = inResidence();
        transferOut(residentId);
        Instant deathAt = time.now();

        BusinessException withoutEvidence = recordDeathFailure(office, residentId, deathDetails(deathAt, null, null));
        assertThat(withoutEvidence.errorCode()).isEqualTo("EVIDENCE_REQUIRED");
        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.HOSPITALIZED);

        long evidence = evidenceFile();
        recordDeath(office, residentId, deathDetails(deathAt, null, evidence));

        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.DECEASED);
        assertThat(((Number) deathRecord(residentId).get("evidence_file_id")).longValue())
                .isEqualTo(evidence);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_DEATH_RECORDED' AND resident_id = ?",
                        Integer.class,
                        residentId))
                .isEqualTo(1);
    }

    @Test
    void FR_069_administrativeStaffCannotRecordADeathInsideTheFacility() throws Exception {
        long residentId = inResidence();

        BusinessException e = recordDeathFailure(office, residentId, deathDetails(time.now(), null, evidenceFile()));

        assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM death_record WHERE resident_id = ?", Integer.class, residentId))
                .isZero();
    }

    @Test
    void FR_069_theDeathRecordIsAppendOnly_andOnlyOneExistsPerResident() throws Exception {
        long residentId = inResidence();
        doctorRecordsDeath(residentId, "Suy tim");
        long recordId = ((Number) deathRecord(residentId).get("id")).longValue();

        assertThatThrownBy(() -> jdbc.update("UPDATE death_record SET place = 'Nơi khác' WHERE id = ?", recordId))
                .as("tài khoản ứng dụng không có quyền UPDATE")
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM death_record WHERE id = ?", recordId))
                .as("tài khoản ứng dụng không có quyền DELETE")
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO death_record (resident_id, death_at, place, discovered_by, confirmed_by_id,"
                                + " history_id, recorded_by, recorded_at) VALUES (?, NOW(6), 'a', 'b', 1, 1, 1, NOW(6))",
                        residentId))
                .hasMessageContaining("uq_death_record_resident");
    }

    @Test
    void FR_069_theCommandNeedsPlaceDiscovererAndConfirmer() throws Exception {
        long residentId = inResidence();
        for (String missing : new String[] {"place", "discoveredBy", "confirmedById", "deathAt"}) {
            ObjectNode details = deathDetails(time.now(), null, null);
            details.remove(missing);

            BusinessException e = recordDeathFailure(doctor, residentId, details);

            assertThat(e.status()).as(missing).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }
        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }
}
