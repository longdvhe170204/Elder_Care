package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.ReasonRequiredException;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.common.file.FileService;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;

/**
 * 001 T045 (FR-047b, Q-342, US4 #10–11): "Ghi nhận qua đời" do Bác sĩ thực hiện khi Đang lưu trú (mất tại viện). Khi
 * Tạm vắng, Hoạt động bên ngoài, Điều trị tại bệnh viện (mất ngoài viện), Bác sĩ hoặc Hành chính được làm; Hành chính
 * bắt buộc kèm {@code evidenceFileId}. Tệp bằng chứng phải tồn tại, chưa gắn, và được gắn vào dòng lịch sử trong chính
 * transaction của lệnh (chủ sở hữu {@code M01.DEATH_EVIDENCE}); lệnh lỗi thì không đổi trạng thái, không gắn tệp.
 */
@Import(FakeFileConfig.class)
class RecordDeathIT extends StatusTestBase {

    private static final String OWNER_TYPE = "M01.DEATH_EVIDENCE";

    @Autowired
    FileApi files;

    @Autowired
    FileService fileService;

    @Autowired
    List<FilePermissionProvider> filePermissions;

    /** Tệp PDF vừa tải lên bởi Hành chính, chưa gắn. */
    private long evidence() {
        return files.upload(
                "giay-bao-tu.pdf",
                FileTypes.PDF,
                "%PDF-1.4 giấy báo tử".getBytes(StandardCharsets.UTF_8),
                office.accountId());
    }

    private Map<String, Object> fileRow(long fileId) {
        return jdbc.queryForMap("SELECT owner_type, owner_id, attached_at FROM file WHERE id = ?", fileId);
    }

    private long lastHistoryId(long residentId) {
        return jdbc.queryForObject(
                "SELECT id FROM resident_status_history WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                residentId);
    }

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
        long file = evidence();
        user.set(office.user());

        BusinessException e =
                failure(() -> statusService.recordDeath(target(id), reason(), details("evidenceFileId", file)));

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

            long file = evidence();
            statusService.recordDeath(target(withEvidence), reason(), details("evidenceFileId", file));
            assertThat(currentStatus(withEvidence)).isEqualTo(ResidentStatus.DECEASED);
            Map<String, Object> row = lastHistory(withEvidence);
            assertThat(row.get("basis_type")).isEqualTo("EVIDENCE_FILE");
            assertThat(((Number) row.get("basis_id")).longValue()).isEqualTo(file);
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

        assertThatThrownBy(() -> statusService.recordDeath(target(id), reason(), noDetails()))
                .isInstanceOf(PermissionDeniedException.class);
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_047b_evidenceIsAttachedToTheHistoryRow_inTheSameCommand() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        long file = evidence();
        user.set(office.user());

        statusService.recordDeath(target(id), reason(), details("evidenceFileId", file));

        Map<String, Object> row = fileRow(file);
        assertThat(row.get("owner_type")).isEqualTo(OWNER_TYPE);
        assertThat(((Number) row.get("owner_id")).longValue()).isEqualTo(lastHistoryId(id));
        assertThat(row.get("attached_at")).isNotNull();
    }

    @Test
    void FR_047b_doctorMayAlsoAttachEvidence() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        long file = evidence();
        user.set(doctor.user());

        statusService.recordDeath(target(id), reason(), details("evidenceFileId", file));

        assertThat(fileRow(file).get("owner_type")).isEqualTo(OWNER_TYPE);
    }

    @Test
    void FR_047b_evidenceFileThatDoesNotExist_isRefused_andNothingChanges() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        int before = historyCount(id);
        user.set(office.user());

        BusinessException e =
                failure(() -> statusService.recordDeath(target(id), reason(), details("evidenceFileId", 987_654_321L)));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.TEMPORARILY_ABSENT);
        assertThat(historyCount(id)).isEqualTo(before);
    }

    @Test
    void FR_047b_evidenceFileAlreadyAttached_isRefused_andNothingChanges() {
        long first = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        long second = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        long file = evidence();
        user.set(office.user());
        statusService.recordDeath(target(first), reason(), details("evidenceFileId", file));
        int before = historyCount(second);

        BusinessException e =
                failure(() -> statusService.recordDeath(target(second), reason(), details("evidenceFileId", file)));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(e.errorCode()).isEqualTo("FILE_ALREADY_ATTACHED");
        assertThat(currentStatus(second)).isEqualTo(ResidentStatus.TEMPORARILY_ABSENT);
        assertThat(historyCount(second)).isEqualTo(before);
    }

    @Test
    void FR_047b_failedCommand_doesNotAttachTheFile() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        long file = evidence();
        user.set(office.user());

        failure(() -> statusService.recordDeath(target(id), reason(), details("evidenceFileId", file)));

        assertThat(fileRow(file).get("attached_at")).isNull();
    }

    @Test
    void NFR_08_evidenceCanBeDownloadedByWhoCanViewTheDeathRecord_notByNurse() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        assignNurse(nurse, id);
        long file = evidence();
        user.set(office.user());
        statusService.recordDeath(target(id), reason(), details("evidenceFileId", file));

        assertThat(filePermissions.stream().anyMatch(p -> p.ownerType().equals(OWNER_TYPE)))
                .as("có FilePermissionProvider cho %s", OWNER_TYPE)
                .isTrue();
        user.set(office.user());
        assertThat(fileService.download(file, office.accountId()).content()).isNotEmpty();
        user.set(doctor.user());
        assertThat(fileService.download(file, doctor.accountId()).content()).isNotEmpty();
        user.set(nurse.user());
        assertThatThrownBy(() -> fileService.download(file, nurse.accountId()))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_046_everyUserCommandNeedsAReason() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(doctor.user());

        assertThatThrownBy(() -> statusService.recordDeath(target(id), new Reason(" "), noDetails()))
                .isInstanceOf(ReasonRequiredException.class);
    }
}
