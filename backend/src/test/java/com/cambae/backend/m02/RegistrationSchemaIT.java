package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * 018 T002 (data-model mục 2 → 5; FR-003, FR-005, DBR-33): bảng khai báo ghi bằng tài khoản ứng dụng chặn được khai báo
 * thứ hai đang xử lý hay Đã xác nhận của cùng người, chuyển trạng thái ngoài bảng FR-005, sửa cột đặt một lần, sửa hay xóa
 * lần nộp (nhóm 3); chỉ tài khoản loại bỏ dữ liệu xóa được.
 */
class RegistrationSchemaIT extends MySqlIntegrationTestBase {

    private static final String TEMP = "TEMPORARY_RESIDENCE_REGISTRATION";
    private static final String STAY = "STAY_NOTIFICATION";
    private static final AtomicLong RESIDENTS = new AtomicLong(918_000_000L);

    @Autowired
    JdbcTemplate jdbc;

    private static long resident() {
        return RESIDENTS.incrementAndGet();
    }

    private long registration(long residentId, String type, String status) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO residence_registration (resident_id, type, due_at, status) VALUES (?, ?,"
                                    + " '2031-03-02 00:00:00', ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, residentId);
                    ps.setString(2, type);
                    ps.setString(3, status);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private void setStatus(long id, String status) {
        jdbc.update("UPDATE residence_registration SET status = ? WHERE id = ?", status, id);
    }

    private void confirm(long id) {
        jdbc.update(
                "UPDATE residence_registration SET status = 'CONFIRMED', expiry_date = '2032-03-01' WHERE id = ?", id);
    }

    private long submission(long registrationId) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var ps = c.prepareStatement(
                            "INSERT INTO registration_submission (registration_id, submitted_date, channel,"
                                    + " receiving_authority, recorded_by, recorded_at) VALUES (?, '2031-03-02',"
                                    + " 'IN_PERSON', 'Công an phường', 7, '2031-03-02 03:00:00')",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, registrationId);
                    return ps;
                },
                key);
        return key.getKey().longValue();
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT status FROM residence_registration WHERE id = ?", String.class, id);
    }

    @Test
    void DBR_33_secondInProgressRegistration_isRejected_inBothInProgressStatuses() {
        long residentId = resident();
        long first = registration(residentId, TEMP, "TO_SUBMIT");

        assertThatThrownBy(() -> registration(residentId, STAY, "TO_SUBMIT")).isInstanceOf(DuplicateKeyException.class);
        setStatus(first, "SUBMITTED");
        assertThatThrownBy(() -> registration(residentId, TEMP, "TO_SUBMIT")).isInstanceOf(DuplicateKeyException.class);
        assertThatCode(() -> registration(resident(), TEMP, "TO_SUBMIT"))
                .as("người khác")
                .doesNotThrowAnyException();
    }

    @Test
    void DBR_33_oneInProgressAndOneConfirmed_canCoexist_butNotTwoConfirmed() {
        long residentId = resident();
        long confirmed = registration(residentId, TEMP, "SUBMITTED");
        confirm(confirmed);
        long renewal = registration(residentId, TEMP, "TO_SUBMIT");
        setStatus(renewal, "SUBMITTED");

        assertThatThrownBy(() -> confirm(renewal)).isInstanceOf(DuplicateKeyException.class);
        assertThat(status(renewal)).isEqualTo("SUBMITTED");
    }

    @Test
    void DBR_33_supersedingTheOldConfirmedFirst_letsTheNewOneBeConfirmed() {
        long residentId = resident();
        long old = registration(residentId, STAY, "SUBMITTED");
        confirm(old);
        long renewal = registration(residentId, STAY, "SUBMITTED");

        setStatus(old, "SUPERSEDED");
        confirm(renewal);

        assertThat(status(old)).isEqualTo("SUPERSEDED");
        assertThat(status(renewal)).isEqualTo("CONFIRMED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM residence_registration WHERE resident_id = ? AND confirmed_key IS NOT NULL",
                        Integer.class,
                        residentId))
                .isOne();
    }

    @Test
    void DBR_33_finishedRegistrations_doNotBlockNewOnes() {
        long residentId = resident();
        long cancelled = registration(residentId, TEMP, "TO_SUBMIT");
        setStatus(cancelled, "CANCELLED");
        long expired = registration(residentId, TEMP, "SUBMITTED");
        confirm(expired);
        setStatus(expired, "EXPIRED");

        assertThatCode(() -> registration(residentId, TEMP, "TO_SUBMIT")).doesNotThrowAnyException();
    }

    @Test
    void FR_003_acceptedTemporaryResidenceRegistration_requiresAnExpiryDate_stayNotificationDoesNot() {
        long temp = registration(resident(), TEMP, "SUBMITTED");
        long stay = registration(resident(), STAY, "SUBMITTED");

        assertThatThrownBy(() -> setStatus(temp, "CONFIRMED")).isInstanceOf(DataAccessException.class);
        assertThat(status(temp)).isEqualTo("SUBMITTED");
        setStatus(stay, "CONFIRMED");
        assertThat(status(stay)).isEqualTo("CONFIRMED");
    }

    @Test
    void FR_005_allowedTransitions_followTheTable() {
        long rejectedThenAccepted = registration(resident(), TEMP, "TO_SUBMIT");
        setStatus(rejectedThenAccepted, "SUBMITTED");
        setStatus(rejectedThenAccepted, "TO_SUBMIT");
        setStatus(rejectedThenAccepted, "SUBMITTED");
        confirm(rejectedThenAccepted);
        setStatus(rejectedThenAccepted, "EXPIRED");
        jdbc.update(
                "UPDATE residence_registration SET status = 'DEREGISTERED', deregistered_at = '2032-04-01 02:00:00',"
                        + " deregistered_by_id = 7, deregistration_evidence_file_id = 99 WHERE id = ?",
                rejectedThenAccepted);
        long cancelledWhileSubmitted = registration(resident(), STAY, "SUBMITTED");
        setStatus(cancelledWhileSubmitted, "CANCELLED");

        assertThat(status(rejectedThenAccepted)).isEqualTo("DEREGISTERED");
        assertThat(status(cancelledWhileSubmitted)).isEqualTo("CANCELLED");
    }

    @Test
    void FR_005_transitionsOutsideTheTable_areRejected() {
        long toSubmit = registration(resident(), STAY, "TO_SUBMIT");
        long confirmed = registration(resident(), STAY, "SUBMITTED");
        setStatus(confirmed, "CONFIRMED");
        long cancelled = registration(resident(), STAY, "TO_SUBMIT");
        setStatus(cancelled, "CANCELLED");

        assertThatThrownBy(() -> setStatus(toSubmit, "CONFIRMED")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> setStatus(toSubmit, "EXPIRED")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> setStatus(confirmed, "TO_SUBMIT")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> setStatus(confirmed, "CANCELLED")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> setStatus(cancelled, "TO_SUBMIT")).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE residence_registration SET basis_note = 'x' WHERE id = ?", cancelled))
                .as("trạng thái cuối không đổi được cột nào")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> setStatus(confirmed, "DEREGISTERED"))
                .as("Đã xóa cần thời điểm, người ghi, bằng chứng")
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_001_typeChangesOnlyWhileToSubmit_andSetOnceColumnsStay() {
        long toSubmit = registration(resident(), TEMP, "TO_SUBMIT");
        jdbc.update(
                "UPDATE residence_registration SET type = 'STAY_NOTIFICATION', local_permanent_resident = TRUE,"
                        + " basis_note = 'Cùng phường' WHERE id = ?",
                toSubmit);
        long submitted = registration(resident(), TEMP, "SUBMITTED");
        confirm(submitted);

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE residence_registration SET type = 'STAY_NOTIFICATION' WHERE id = ?", submitted))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE residence_registration SET expiry_date = '2033-01-01' WHERE id = ?", submitted))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE residence_registration SET resident_id = resident_id + 1 WHERE id = ?", toSubmit))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_003_submissionResult_isSetOnce_andRejectionNeedsAReason() {
        long registrationId = registration(resident(), TEMP, "SUBMITTED");
        long accepted = submission(registrationId);
        long rejected = submission(registrationId);

        jdbc.update("UPDATE registration_submission SET result = 'ACCEPTED' WHERE id = ?", accepted);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE registration_submission SET result = 'REJECTED', rejection_reason = 'x' WHERE id = ?",
                        accepted))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE registration_submission SET result = 'REJECTED' WHERE id = ?", rejected))
                .as("từ chối không lý do")
                .isInstanceOf(DataAccessException.class);
        jdbc.update(
                "UPDATE registration_submission SET result = 'REJECTED', rejection_reason = 'Thiếu giấy tờ' WHERE id = ?",
                rejected);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE registration_submission SET receiving_authority = 'Khác' WHERE id = ?", rejected))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE registration_submission SET rejection_reason = 'Khác' WHERE id = ?", rejected))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_010_registrationAndSubmission_areDeletedOnlyByTheRetentionAccount() throws SQLException {
        long registrationId = registration(resident(), STAY, "TO_SUBMIT");
        long submissionId = submission(registrationId);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM registration_submission WHERE id = ?", submissionId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM residence_registration WHERE id = ?", registrationId))
                .isInstanceOf(DataAccessException.class);
        try (Connection retention = retentionConnection();
                Statement st = retention.createStatement()) {
            assertThat(st.executeUpdate("DELETE FROM registration_submission WHERE id = " + submissionId))
                    .isOne();
            assertThat(st.executeUpdate("DELETE FROM residence_registration WHERE id = " + registrationId))
                    .isOne();
        }
    }

    @Test
    void Z_03_obligation_isOnePerResidentTriggerAndSource() {
        long residentId = resident();
        String insert = "INSERT INTO registration_obligation (resident_id, trigger_type, trigger_at, due_at, source_id)"
                + " VALUES (?, ?, '2031-03-01 02:00:00', '2031-03-02 02:00:00', ?)";
        jdbc.update(insert, residentId, "ADMISSION_COMPLETED", 501L);

        assertThatThrownBy(() -> jdbc.update(insert, residentId, "ADMISSION_COMPLETED", 501L))
                .isInstanceOf(DuplicateKeyException.class);
        jdbc.update(insert, residentId, "RENEWAL_ADDENDUM", 501L);
        jdbc.update(insert, residentId, "ADMISSION_COMPLETED", 502L);
    }

    @Test
    void Z_06_reminder_isOncePerTypeResidentAndDay() {
        long residentId = resident();
        String insert = "INSERT IGNORE INTO registration_reminder (type, resident_id, `date`) VALUES (?, ?, ?)";

        assertThat(jdbc.update(insert, "OVERDUE", residentId, "2031-03-03")).isOne();
        assertThat(jdbc.update(insert, "OVERDUE", residentId, "2031-03-03")).isZero();
        assertThat(jdbc.update(insert, "OVERDUE", residentId, "2031-03-04")).isOne();
        assertThat(jdbc.update(insert, "CREATE", residentId, "2031-03-03")).isOne();
    }

    @Test
    void FR_001_registrationResident_isOnePerResident() {
        long residentId = resident();
        jdbc.update("INSERT INTO registration_resident (resident_id) VALUES (?)", residentId);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO registration_resident (resident_id) VALUES (?)", residentId))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT local_permanent_resident FROM registration_resident WHERE resident_id = ?",
                        Boolean.class,
                        residentId))
                .isFalse();
    }
}
