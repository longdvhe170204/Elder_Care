package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.admission.internal.registration.domain.ObligationTrigger;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationStatus;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationType;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionChannel;
import com.eldercare.backend.admission.internal.registration.domain.SubmissionResult;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationObligationStore;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationResidentStore;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationSubmissionStore;
import com.eldercare.backend.admission.internal.registration.persistence.ResidenceRegistrationStore;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 018 T002 (data-model mục 0.1, 3 → 5; R-15; FR-003, FR-005, DBR-33): khai báo ở {@code simple_record}, lần nộp ở {@code
 * record_event} chặn được khai báo thứ hai đang xử lý hay Đã xác nhận của cùng người ({@code unique_key}), chuyển trạng thái
 * ngoài bảng FR-005 (store), sửa dòng đã khóa ở trạng thái cuối, sửa hay xóa lần nộp (nhóm 3); chỉ tài khoản loại bỏ dữ liệu
 * xóa được.
 */
class RegistrationStoreIT extends MySqlIntegrationTestBase {

    private static final AtomicLong RESIDENTS = new AtomicLong(918_000_000L);
    private static final LocalDate EXPIRY = LocalDate.of(2032, 3, 1);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    BusinessTime time;

    @Autowired
    ResidenceRegistrationStore registrations;

    @Autowired
    RegistrationSubmissionStore submissions;

    @Autowired
    RegistrationObligationStore obligations;

    @Autowired
    RegistrationResidentStore residentStore;

    private static long resident() {
        return RESIDENTS.incrementAndGet();
    }

    private <T> T tx(Supplier<T> work) {
        return transaction.execute(status -> work.get());
    }

    private long registration(long residentId, RegistrationType type) {
        return tx(() -> registrations.insertToSubmit(residentId, type, false, null, time.now(), false, null, null, 7));
    }

    private long registration(long residentId, RegistrationType type, RegistrationStatus status) {
        long id = registration(residentId, type);
        if (status == RegistrationStatus.TO_SUBMIT) {
            return id;
        }
        submit(residentId, id);
        if (status == RegistrationStatus.SUBMITTED) {
            return id;
        }
        confirm(id);
        if (status == RegistrationStatus.EXPIRED) {
            assertThat(tx(() -> registrations.markExpired(id))).isTrue();
        }
        return id;
    }

    private long submit(long residentId, long registrationId) {
        return tx(() -> {
            long submission = submissions.add(
                    residentId,
                    registrationId,
                    time.today(),
                    SubmissionChannel.IN_PERSON,
                    "Công an phường",
                    null,
                    7,
                    time.now());
            assertThat(registrations.markSubmitted(registrationId, submission)).isTrue();
            return submission;
        });
    }

    private void confirm(long id) {
        assertThat(tx(() -> registrations.markConfirmed(id, EXPIRY, 99))).isTrue();
    }

    private RegistrationStatus status(long id) {
        return registrations.find(id).orElseThrow().status();
    }

    @Test
    void DBR_33_secondInProgressRegistration_isRejected_inBothInProgressStatuses() {
        long residentId = resident();
        long first = registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);

        assertThatThrownBy(() -> registration(residentId, RegistrationType.STAY_NOTIFICATION))
                .isInstanceOf(ConditionNotMetException.class);
        submit(residentId, first);
        assertThatThrownBy(() -> registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION))
                .isInstanceOf(ConditionNotMetException.class);
        assertThatCode(() -> registration(resident(), RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION))
                .as("người khác")
                .doesNotThrowAnyException();
        assertThat(RegistrationSql.registrationCount(jdbc, residentId)).isOne();
    }

    @Test
    void DBR_33_oneInProgressAndOneConfirmed_canCoexist_butNotTwoConfirmed() {
        long residentId = resident();
        registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION, RegistrationStatus.CONFIRMED);
        long renewal = registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        submit(residentId, renewal);

        assertThatThrownBy(() -> confirm(renewal)).isInstanceOf(ConditionNotMetException.class);
        assertThat(status(renewal)).isEqualTo(RegistrationStatus.SUBMITTED);
        assertThat(RegistrationSql.confirmedCount(jdbc, residentId)).isOne();
    }

    @Test
    void DBR_33_supersedingTheOldConfirmedFirst_letsTheNewOneBeConfirmed() {
        long residentId = resident();
        long old = registration(residentId, RegistrationType.STAY_NOTIFICATION, RegistrationStatus.CONFIRMED);
        long renewal = registration(residentId, RegistrationType.STAY_NOTIFICATION);
        submit(residentId, renewal);

        assertThat(tx(() -> registrations.markSuperseded(old) && registrations.markConfirmed(renewal, EXPIRY, 99)))
                .isTrue();

        assertThat(status(old)).isEqualTo(RegistrationStatus.SUPERSEDED);
        assertThat(status(renewal)).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(RegistrationSql.confirmedCount(jdbc, residentId)).isOne();
    }

    @Test
    void DBR_33_finishedRegistrations_doNotBlockNewOnes() {
        long residentId = resident();
        long cancelled = registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        assertThat(tx(() -> registrations.markCancelled(cancelled))).isTrue();
        registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION, RegistrationStatus.EXPIRED);

        assertThatCode(() -> registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION))
                .doesNotThrowAnyException();
    }

    @Test
    void FR_005_allowedTransitions_followTheTable() {
        long residentId = resident();
        long id = registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        submit(residentId, id);
        assertThat(tx(() -> registrations.markRejected(id, time.now()))).isTrue();
        assertThat(status(id)).isEqualTo(RegistrationStatus.TO_SUBMIT);
        submit(residentId, id);
        confirm(id);
        assertThat(tx(() -> registrations.markExpired(id))).isTrue();
        assertThat(tx(() -> registrations.markDeregistered(id, time.now(), 7, 99)))
                .isTrue();
        long cancelledWhileSubmitted =
                registration(resident(), RegistrationType.STAY_NOTIFICATION, RegistrationStatus.SUBMITTED);
        assertThat(tx(() -> registrations.markCancelled(cancelledWhileSubmitted)))
                .isTrue();

        assertThat(status(id)).isEqualTo(RegistrationStatus.DEREGISTERED);
        assertThat(status(cancelledWhileSubmitted)).isEqualTo(RegistrationStatus.CANCELLED);
    }

    @Test
    void FR_005_transitionsOutsideTheTable_areRefused_andChangeNothing() {
        long toSubmit = registration(resident(), RegistrationType.STAY_NOTIFICATION);
        long confirmed = registration(resident(), RegistrationType.STAY_NOTIFICATION, RegistrationStatus.CONFIRMED);
        long cancelled = registration(resident(), RegistrationType.STAY_NOTIFICATION);
        assertThat(tx(() -> registrations.markCancelled(cancelled))).isTrue();

        assertThat(tx(() -> registrations.markConfirmed(toSubmit, EXPIRY, 99))).isFalse();
        assertThat(tx(() -> registrations.markExpired(toSubmit))).isFalse();
        assertThat(tx(() -> registrations.markRejected(toSubmit, time.now()))).isFalse();
        assertThat(tx(() -> registrations.markSubmitted(confirmed, 1))).isFalse();
        assertThat(tx(() -> registrations.markCancelled(confirmed))).isFalse();
        assertThat(tx(() -> registrations.markSubmitted(cancelled, 1))).isFalse();
        assertThat(tx(() -> registrations.confirmLocalPermanentResident(cancelled, "x")))
                .isFalse();
        assertThat(tx(() -> registrations.markDeregistered(toSubmit, time.now(), 7, 99)))
                .as("Đã xóa chỉ từ Đã xác nhận, Hết hạn")
                .isFalse();
        assertThat(status(toSubmit)).isEqualTo(RegistrationStatus.TO_SUBMIT);
        assertThat(status(confirmed)).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(status(cancelled)).isEqualTo(RegistrationStatus.CANCELLED);
    }

    @Test
    void FR_005_finalStatusRows_areLockedAtTheDatabase() {
        long cancelled = registration(resident(), RegistrationType.STAY_NOTIFICATION);
        assertThat(tx(() -> registrations.markCancelled(cancelled))).isTrue();
        long open = registration(resident(), RegistrationType.STAY_NOTIFICATION);

        assertThatThrownBy(() -> jdbc.update("UPDATE simple_record SET status = 'TO_SUBMIT' WHERE id = ?", cancelled))
                .as("trạng thái cuối khóa dòng")
                .isInstanceOf(DataAccessException.class);
        assertThatCode(() -> RegistrationSql.forceStatus(jdbc, open, "TO_SUBMIT"))
                .doesNotThrowAnyException();
    }

    @Test
    void FR_001_typeChangesOnlyWhileToSubmit_andTheRestOfTheContentStays() {
        long toSubmit = registration(resident(), RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        long submitted = registration(
                resident(), RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION, RegistrationStatus.SUBMITTED);

        assertThat(tx(() -> registrations.confirmLocalPermanentResident(toSubmit, "Cùng phường")))
                .isTrue();
        assertThat(tx(() -> registrations.confirmLocalPermanentResident(submitted, "Cùng phường")))
                .isFalse();

        var changed = registrations.find(toSubmit).orElseThrow();
        assertThat(changed.type()).isEqualTo(RegistrationType.STAY_NOTIFICATION);
        assertThat(changed.localPermanentResident()).isTrue();
        assertThat(changed.basisNote()).isEqualTo("Cùng phường");
        assertThat(registrations.find(submitted).orElseThrow().type())
                .isEqualTo(RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
    }

    @Test
    void FR_003_submissionResult_isSetOnce_andTheConfirmedRowIsAppendOnly() {
        long residentId = resident();
        long registrationId = registration(residentId, RegistrationType.TEMPORARY_RESIDENCE_REGISTRATION);
        long accepted = submit(residentId, registrationId);

        assertThat(tx(() -> submissions.setResult(accepted, SubmissionResult.ACCEPTED, null, time.now())))
                .isTrue();
        assertThat(tx(() -> submissions.setResult(accepted, SubmissionResult.REJECTED, "x", time.now())))
                .as("kết quả đặt một lần")
                .isFalse();
        assertThat(submissions.find(accepted).orElseThrow().result()).isEqualTo(SubmissionResult.ACCEPTED);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE record_event SET content = JSON_SET(content, '$.dossierCode', 'Khác') WHERE id = ?",
                        accepted))
                .hasMessageContaining("append-only once confirmed");
    }

    @Test
    void FR_010_registrationAndSubmission_areDeletedOnlyByTheRetentionAccount() throws SQLException {
        long residentId = resident();
        long registrationId = registration(residentId, RegistrationType.STAY_NOTIFICATION);
        long submissionId = submit(residentId, registrationId);
        long cancelledId = registration(resident(), RegistrationType.STAY_NOTIFICATION);
        assertThat(tx(() -> registrations.markCancelled(cancelledId))).isTrue();

        assertThatThrownBy(() -> jdbc.update("DELETE FROM record_event WHERE id = ?", submissionId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM simple_record WHERE id = ?", registrationId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM simple_record WHERE id = ?", cancelledId))
                .as("dòng đã khóa")
                .isInstanceOf(DataAccessException.class);
        try (Connection retention = retentionConnection();
                Statement st = retention.createStatement()) {
            assertThat(st.executeUpdate("DELETE FROM record_event WHERE id = " + submissionId))
                    .isOne();
            assertThat(st.executeUpdate(
                            "DELETE FROM simple_record WHERE id IN (" + registrationId + ", " + cancelledId + ")"))
                    .isEqualTo(2);
        }
    }

    @Test
    void Z_03_obligation_isOnePerResidentTriggerAndSource() {
        long residentId = resident();
        boolean first = tx(() ->
                obligations.record(residentId, ObligationTrigger.ADMISSION_COMPLETED, time.now(), time.now(), 501L));

        assertThat(first).isTrue();
        assertThat(tx(() -> obligations.record(
                        residentId, ObligationTrigger.ADMISSION_COMPLETED, time.now(), time.now(), 501L)))
                .as("giao lại sự kiện")
                .isFalse();
        assertThat(tx(() -> obligations.record(
                        residentId, ObligationTrigger.RENEWAL_ADDENDUM, time.now(), time.now(), 501L)))
                .isTrue();
        assertThat(tx(() -> obligations.record(
                        residentId, ObligationTrigger.ADMISSION_COMPLETED, time.now(), time.now(), 502L)))
                .isTrue();
        assertThat(RegistrationSql.obligationCount(jdbc, residentId)).isEqualTo(3);
        // Người cao tuổi giả: hủy nghĩa vụ để danh sách cần làm của test khác không tra họ tên người không có
        assertThat(tx(() -> obligations.cancelOpen(residentId, time.now()))).isEqualTo(3);
        assertThat(RegistrationSql.openObligationCount(jdbc, residentId)).isZero();
    }

    @Test
    void FR_001_residentFlag_isOneRowPerResident() {
        long residentId = resident();

        assertThat(tx(() -> residentStore.lock(residentId))).isFalse();
        transaction.executeWithoutResult(
                status -> residentStore.confirmLocalPermanentResident(residentId, "Căn cứ", 7, time.now()));
        transaction.executeWithoutResult(
                status -> residentStore.confirmLocalPermanentResident(residentId, "Căn cứ mới", 8, time.now()));

        assertThat(RegistrationSql.flagRows(jdbc, residentId)).isOne();
        assertThat(tx(() -> residentStore.lock(residentId))).isTrue();
        assertThat(residentStore.confirmedBy(residentId)).contains(8L);
        transaction.executeWithoutResult(status -> residentStore.removeLocalPermanentResident(residentId));
        assertThat(RegistrationSql.localPermanentResident(jdbc, residentId)).isFalse();
        assertThat(residentStore.confirmedBy(residentId)).isEmpty();
    }
}
