package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.domain.WishStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 001 T067 (FR-023a, FR-023b, DBR-34; US7 #1, #5): ghi phiếu nguyện vọng cuối đời là ghi phiên bản. Phiên bản mới thay
 * bản Hiệu lực cũ trong cùng lệnh; cơ sở dữ liệu giữ tối đa một bản Hiệu lực, chỉ cho đổi Hiệu lực → Được thay thế và
 * không xóa. 007 ghi phiên bản xác nhận lại lúc nguy kịch qua {@link EndOfLifeWishApi} trong transaction của nó.
 */
class EndOfLifeWishVersionIT extends WishTestBase {

    @Autowired
    EndOfLifeWishApi api;

    @Autowired
    TransactionTemplate tx;

    @Test
    void US7_firstVersion_isActive_withWriterAndAudit() {
        long residentId = newResident();

        EndOfLifeWish wish = record(
                command(residentId).choice(WishChoice.TRANSFER_TO_HOSPITAL).build());

        assertThat(wish.status()).isEqualTo(WishStatus.ACTIVE);
        assertThat(wish.choice()).isEqualTo(WishChoice.TRANSFER_TO_HOSPITAL);
        assertThat(wish.recordedBy()).isEqualTo(nurse.accountId());
        assertThat(wish.source()).isEqualTo(WishRecordSource.ADMISSION);
        assertThat(wish.assessingDoctorId()).isEqualTo(doctor.accountId());
        assertThat(activeWishCount(residentId)).isEqualTo(1);
        assertThat(wishAuditActions(wish.id())).contains("END_OF_LIFE_WISH_RECORDED");
    }

    @Test
    void FR_023b_newVersion_supersedesTheActiveOne_andKeepsHistory() {
        long residentId = newResident();
        EndOfLifeWish first = record(
                command(residentId).choice(WishChoice.TRANSFER_TO_HOSPITAL).build());

        EndOfLifeWish second = recordAs(
                WishRecordSource.FAMILY_REQUEST,
                command(residentId).choice(WishChoice.RETURN_HOME).build());

        assertThat(activeWishCount(residentId)).isEqualTo(1);
        var summary = wishes.get(target(residentId));
        assertThat(summary.hasWish()).isTrue();
        assertThat(summary.current()).hasValueSatisfying(w -> assertThat(w.id()).isEqualTo(second.id()));
        assertThat(summary.history()).extracting(EndOfLifeWish::id).containsExactly(second.id(), first.id());
        EndOfLifeWish old = summary.history().get(1);
        assertThat(old.status()).isEqualTo(WishStatus.SUPERSEDED);
        assertThat(old.choice()).as("nội dung bản cũ không đổi").isEqualTo(WishChoice.TRANSFER_TO_HOSPITAL);
        assertThat(jdbc.queryForObject(
                        "SELECT superseded_by_id FROM end_of_life_wish WHERE id = ?", Long.class, first.id()))
                .isEqualTo(second.id());
        assertThat(wishAuditActions(first.id())).contains("END_OF_LIFE_WISH_SUPERSEDED");
    }

    @Test
    void DBR_34_secondActiveVersionIsRejectedByTheDatabase() {
        long residentId = newResident();
        EndOfLifeWish wish = record(command(residentId).build());

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO end_of_life_wish (" + COPY_COLUMNS + ") SELECT " + COPY_COLUMNS
                                + " FROM end_of_life_wish WHERE id = ?",
                        wish.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_end_of_life_wish_active");
    }

    @Test
    void DBR_34_signerMustMatchSelfDeterminationAssessment_atTheDatabase() {
        long residentId = newResident();
        EndOfLifeWish wish = record(command(residentId).build());

        // bản sao đã Được thay thế (không vướng UNIQUE) nhưng nhận định "không tự quyết được" mà người ký vẫn là người
        // cao tuổi
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO end_of_life_wish (" + COPY_COLUMNS + ") SELECT "
                                + COPY_COLUMNS
                                        .replace("can_self_determine", "FALSE")
                                        .replace("status", "'SUPERSEDED'")
                                + " FROM end_of_life_wish WHERE id = ?",
                        wish.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("ck_end_of_life_wish_signer");
    }

    @Test
    void FR_023b_savedVersionsCannotBeEditedOrDeleted() {
        long residentId = newResident();
        EndOfLifeWish wish = record(command(residentId)
                .opinion(OpinionSourceType.OBSERVING_STAFF, null, "Ý kiến")
                .build());
        EndOfLifeWish newer =
                recordAs(WishRecordSource.FAMILY_REQUEST, command(residentId).build());

        assertThatThrownBy(() ->
                        jdbc.update("UPDATE end_of_life_wish SET choice = 'RETURN_HOME' WHERE id = ?", newer.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE end_of_life_wish SET status = 'ACTIVE' WHERE id = ?", wish.id()))
                .as("không chuyển ngược Được thay thế → Hiệu lực")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM end_of_life_wish WHERE id = ?", wish.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE end_of_life_wish_opinion SET content = 'sửa' WHERE wish_id = ?", wish.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM end_of_life_wish_opinion WHERE wish_id = ?", wish.id()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_023f_closedProfileCannotGetANewVersion_butKeepsTheActiveOne() {
        long residentId = newResident();
        EndOfLifeWish wish = record(command(residentId).build());
        setStatus(residentId, "RESIDENCY_ENDED");

        var error = failure(() ->
                recordAs(WishRecordSource.FAMILY_REQUEST, command(residentId).build()));

        assertThat(error.errorCode()).isEqualTo("PROFILE_CLOSED");
        assertThat(wishes.get(target(residentId)).current())
                .hasValueSatisfying(w -> assertThat(w.id()).isEqualTo(wish.id()));
    }

    @Test
    void BR_M05_16_recordVersionFromTheEmergencyModule_replacesTheActiveVersionInItsTransaction() {
        long residentId = newResident();
        EndOfLifeWish first = record(command(residentId).build());
        assignNurse(nurse, residentId);
        user.set(nurse.user());

        long id = tx.execute(status -> api.recordVersion(
                command(residentId).choice(WishChoice.RETURN_HOME).build(), WishRecordSource.CRITICAL_RECONFIRMATION));

        EndOfLifeWish current = wishes.get(target(residentId)).current().orElseThrow();
        assertThat(current.id()).isEqualTo(id).isNotEqualTo(first.id());
        assertThat(current.source()).isEqualTo(WishRecordSource.CRITICAL_RECONFIRMATION);
        assertThat(api.current(residentId)).hasValueSatisfying(view -> {
            assertThat(view.id()).isEqualTo(id);
            assertThat(view.choice()).isEqualTo(WishChoice.RETURN_HOME);
        });
        assertThat(activeWishCount(residentId)).isEqualTo(1);
    }

    @Test
    void BR_M05_16_recordVersionOutsideATransactionIsRefused() {
        long residentId = newResident();
        assignNurse(nurse, residentId);
        user.set(nurse.user());

        assertThatThrownBy(
                        () -> api.recordVersion(command(residentId).build(), WishRecordSource.CRITICAL_RECONFIRMATION))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private static final String COPY_COLUMNS = "resident_id, choice, first_contact, note, respondent_type,"
            + " respondent_family_member_id, relationship, assessing_doctor_id, can_self_determine, assessed_at,"
            + " support_method, review_date, signer_type, signer_family_member_id, representative_selection,"
            + " court_decision_number, court_decision_date, court_decision_court, court_decision_file_id, signed_at,"
            + " signed_copy_file_id, source, status, recorded_by, recorded_at";
}
