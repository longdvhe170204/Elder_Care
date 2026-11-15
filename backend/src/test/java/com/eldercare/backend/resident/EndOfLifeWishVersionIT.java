package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.dto.WishChoice;
import com.eldercare.backend.resident.api.dto.WishRecordSource;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import com.eldercare.backend.resident.internal.wish.domain.WishStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 001 T067 (FR-023a, FR-023b, DBR-34; US7 #1, #5): ghi phiếu nguyện vọng cuối đời là ghi phiên bản. Phiên bản mới thay
 * bản Hiệu lực cũ trong cùng lệnh; cơ sở dữ liệu giữ tối đa một bản Hiệu lực, chỉ cho đổi Hiệu lực → Được thay thế và
 * không xóa. 007 ghi phiên bản xác nhận lại lúc nguy kịch qua {@link EndOfLifeWishApi} trong transaction của nó.
 */
@Tag("BR-06")
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
    void DBR_34_secondActiveVersionIsRejectedByTheDatabase() {
        long residentId = newResident();
        EndOfLifeWish wish = record(command(residentId).build());

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO wish (kind, " + COPY_COLUMNS + ") SELECT kind, " + COPY_COLUMNS
                                + " FROM wish WHERE id = ?",
                        wish.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_wish_active");
    }

    private static final String COPY_COLUMNS = "resident_id, choice, first_contact, note, respondent_type,"
            + " respondent_family_member_id, relationship, assessing_doctor_id, can_self_determine, assessed_at,"
            + " support_method, review_date, signer_type, signer_family_member_id, representative_selection,"
            + " court_decision_number, court_decision_date, court_decision_court, court_decision_file_id, signed_at,"
            + " signed_copy_file_id, source, status, recorded_by, recorded_at";
}
