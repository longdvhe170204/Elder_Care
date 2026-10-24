package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m01.api.EndOfLifeWishApi;
import com.cambae.backend.m01.api.dto.HealthInsurance;
import com.cambae.backend.m01.api.dto.OpinionSourceType;
import com.cambae.backend.m01.api.dto.PreferredHospital;
import com.cambae.backend.m01.api.dto.TreatmentEscort;
import com.cambae.backend.m01.internal.wish.domain.TreatmentWish;
import com.cambae.backend.m01.internal.wish.domain.TreatmentWishStatus;
import com.cambae.backend.m01.internal.wish.service.TreatmentWishService.Result;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

/**
 * 001 T077 (FR-023j, FR-023k, FR-023g, DBR-39, BR-M01-16; US7 #9, #10; SC-014): phiếu nguyện vọng khi điều trị có phiên
 * bản như phiếu cuối đời nhưng bản cũ chuyển Hết hiệu lực. Một hoặc hai bệnh viện ưu tiên; người cần báo là người thân
 * có quan hệ Hiệu lực (0..n); hỏi gia đình trước mà không có người cần báo thì chỉ cảnh báo; ý kiến gia đình khác lưu
 * từng dòng có nguồn; {@code currentTreatmentWish} trả đủ trường cho 007.
 */
class TreatmentWishVersionIT extends WishTestBase {

    @Autowired
    EndOfLifeWishApi api;

    private Result record(long residentId, TreatmentCommandBuilder builder) {
        assignNurse(nurse, residentId);
        user.set(nurse.user());
        return treatments.record(target(residentId), builder.build());
    }

    @Test
    void US7_9_firstVersion_isActive_withWriterAndAudit() {
        long residentId = newResident();
        long child = relatedFamilyMember(residentId);

        TreatmentWish wish =
                record(residentId, new TreatmentCommandBuilder().notify(child)).wish();

        assertThat(wish.status()).isEqualTo(TreatmentWishStatus.ACTIVE);
        assertThat(wish.preferredHospitals())
                .extracting(PreferredHospital::name)
                .containsExactly("Bệnh viện X");
        assertThat(wish.askFamilyFirst()).isTrue();
        assertThat(wish.notifyFamilyMemberIds()).containsExactly(child);
        assertThat(wish.accompaniedBy()).isEqualTo(TreatmentEscort.FAMILY);
        assertThat(wish.recordedBy()).isEqualTo(nurse.accountId());
        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type = 'M01.TREATMENT_WISH' AND entity_id = ?",
                        String.class,
                        wish.id()))
                .contains("TREATMENT_WISH_RECORDED");
    }

    @Test
    void US7_10_newVersion_expiresTheOldOne_whichStaysReadable() {
        long residentId = newResident();
        TreatmentWish first = record(residentId, new TreatmentCommandBuilder()).wish();

        TreatmentWish second = record(
                        residentId,
                        new TreatmentCommandBuilder().hospitals(new PreferredHospital("Bệnh viện Y", "5 Nguyễn Huệ")))
                .wish();

        var summary = treatments.get(target(residentId));
        assertThat(summary.active()).hasValueSatisfying(w -> assertThat(w.id()).isEqualTo(second.id()));
        assertThat(summary.history()).extracting(TreatmentWish::id).containsExactly(second.id(), first.id());
        assertThat(summary.history().get(1).status()).isEqualTo(TreatmentWishStatus.EXPIRED);
        assertThat(summary.history().get(1).preferredHospitals().get(0).name())
                .as("nội dung bản cũ không đổi")
                .isEqualTo("Bệnh viện X");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM treatment_wish WHERE resident_id = ? AND status = 'ACTIVE'",
                        Integer.class,
                        residentId))
                .isEqualTo(1);
    }

    @Test
    void DBR_39_secondActiveVersionIsRejectedByTheDatabase() {
        long residentId = newResident();
        TreatmentWish wish = record(residentId, new TreatmentCommandBuilder()).wish();

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO treatment_wish (resident_id, hospital1_name, hospital1_address, ask_family_first,"
                                + " escort, respondent_type, status, recorded_by, recorded_at)"
                                + " SELECT resident_id, hospital1_name, hospital1_address, ask_family_first, escort,"
                                + " respondent_type, status, recorded_by, recorded_at FROM treatment_wish WHERE id = ?",
                        wish.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_treatment_wish_active");
    }

    @Test
    void FR_023k_savedVersionsAndTheirChildRowsCannotBeEditedOrDeleted() {
        long residentId = newResident();
        long child = relatedFamilyMember(residentId);
        TreatmentWish first = record(
                        residentId,
                        new TreatmentCommandBuilder()
                                .notify(child)
                                .opinion(OpinionSourceType.OBSERVING_STAFF, null, "Cụ ngại bệnh viện lớn"))
                .wish();
        TreatmentWish second = record(residentId, new TreatmentCommandBuilder()).wish();

        assertThatThrownBy(() -> jdbc.update("UPDATE treatment_wish SET note = 'sửa' WHERE id = ?", second.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE treatment_wish SET status = 'ACTIVE' WHERE id = ?", first.id()))
                .as("không chuyển ngược Hết hiệu lực → Hiệu lực")
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM treatment_wish WHERE id = ?", first.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update("DELETE FROM treatment_wish_notify WHERE treatment_wish_id = ?", first.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE treatment_wish_opinion SET content = 'sửa' WHERE treatment_wish_id = ?", first.id()))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(
                        () -> jdbc.update("DELETE FROM treatment_wish_opinion WHERE treatment_wish_id = ?", first.id()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_023j_atLeastOneAndAtMostTwoHospitals_eachWithNameAndAddress() {
        long residentId = newResident();
        var tooMany = new PreferredHospital[] {
            new PreferredHospital("A", "a"), new PreferredHospital("B", "b"), new PreferredHospital("C", "c")
        };

        assertThat(failure(() -> record(residentId, new TreatmentCommandBuilder().hospitals()))
                        .errorCode())
                .isEqualTo("PREFERRED_HOSPITAL_REQUIRED");
        assertThat(failure(() -> record(
                                residentId, new TreatmentCommandBuilder().hospitals(new PreferredHospital("A", " "))))
                        .errorCode())
                .as("thiếu địa chỉ")
                .isEqualTo("PREFERRED_HOSPITAL_REQUIRED");
        var error = failure(() -> record(residentId, new TreatmentCommandBuilder().hospitals(tooMany)));
        assertThat(error.errorCode()).isEqualTo("TOO_MANY_PREFERRED_HOSPITALS");
        assertThat(error.status().value()).isEqualTo(422);
        assertThat(treatments.get(target(residentId)).active()).isEmpty();
        record(
                residentId,
                new TreatmentCommandBuilder()
                        .hospitals(new PreferredHospital("A", "a"), new PreferredHospital("B", "b")));
        assertThat(treatments.get(target(residentId)).active().orElseThrow().preferredHospitals())
                .hasSize(2);
    }

    @Test
    void FR_023j_healthInsuranceIsOptional_andStoredWhenGiven() {
        long residentId = newResident();
        TreatmentWish without =
                record(residentId, new TreatmentCommandBuilder()).wish();
        assertThat(without.healthInsurance()).isNull();

        TreatmentWish with = record(
                        residentId,
                        new TreatmentCommandBuilder()
                                .insurance(new HealthInsurance(
                                        "Bệnh viện Quận 1", "DN4797912345678", LocalDate.of(2027, 12, 31))))
                .wish();

        assertThat(with.healthInsurance().cardNumber()).isEqualTo("DN4797912345678");
        assertThat(with.healthInsurance().initialRegistrationPlace()).isEqualTo("Bệnh viện Quận 1");
        assertThat(with.healthInsurance().cardExpiry()).isEqualTo(LocalDate.of(2027, 12, 31));
    }

    @Test
    void FR_023j_notifiedFamilyMemberWithoutAnActiveRelationship_is409() {
        long residentId = newResident();
        long stranger = newFamilyMember();

        var error = failure(() -> record(residentId, new TreatmentCommandBuilder().notify(stranger)));

        assertThat(error.errorCode()).isEqualTo("INVALID_FAMILY_MEMBER");
        assertThat(error.status().value()).isEqualTo(409);
        assertThat(treatments.get(target(residentId)).active()).isEmpty();
    }

    @Test
    void FR_023j_askingTheFamilyFirstWithoutAnyoneToNotify_savesWithAWarning() {
        long residentId = newResident();

        Result noOne = record(residentId, new TreatmentCommandBuilder().askFamilyFirst(true));
        assertThat(noOne.warnings()).extracting(w -> w.code()).containsExactly("NO_FAMILY_MEMBER_TO_NOTIFY");
        assertThat(noOne.wish().status()).isEqualTo(TreatmentWishStatus.ACTIVE);

        Result notAsking = record(residentId, new TreatmentCommandBuilder().askFamilyFirst(false));
        assertThat(notAsking.warnings()).isEmpty();

        Result withPerson = record(residentId, new TreatmentCommandBuilder().notify(relatedFamilyMember(residentId)));
        assertThat(withPerson.warnings()).isEmpty();
    }

    @Test
    void FR_023g_familyOpinionsAreStoredOnePerRowWithSource_andInvalidSourceIs422() {
        long residentId = newResident();
        long son = relatedFamilyMember(residentId);
        long stranger = newFamilyMember();

        TreatmentWish wish = record(
                        residentId,
                        new TreatmentCommandBuilder()
                                .opinion(OpinionSourceType.FAMILY_MEMBER, son, "Muốn chuyển bệnh viện Z")
                                .opinion(OpinionSourceType.OBSERVING_STAFF, null, "Cụ hay lo chi phí"))
                .wish();
        assertThat(wish.otherOpinions()).hasSize(2);
        assertThat(wish.otherOpinions().get(0).source().familyMemberId()).isEqualTo(son);

        var error = failure(() -> record(
                residentId,
                new TreatmentCommandBuilder().opinion(OpinionSourceType.FAMILY_MEMBER, stranger, "Không có quan hệ")));
        assertThat(error.errorCode()).isEqualTo("INVALID_OPINION_SOURCE");
        assertThat(error.status().value()).isEqualTo(422);
        assertThat(treatments.get(target(residentId)).active().orElseThrow().id())
                .isEqualTo(wish.id());
    }

    @Test
    void FR_023g_respondentSourceIsValidatedLikeAnyOpinionSource() {
        long residentId = newResident();
        long stranger = newFamilyMember();

        var error = failure(() -> record(
                residentId, new TreatmentCommandBuilder().respondent(OpinionSourceType.FAMILY_MEMBER, stranger)));

        assertThat(error.errorCode()).isEqualTo("INVALID_OPINION_SOURCE");
    }

    @Test
    void FR_023j_closedProfileCannotGetANewVersion() {
        long residentId = newResident();
        TreatmentWish wish = record(residentId, new TreatmentCommandBuilder()).wish();
        setStatus(residentId, "DECEASED");

        var error = failure(() -> record(residentId, new TreatmentCommandBuilder()));

        assertThat(error.errorCode()).isEqualTo("PROFILE_CLOSED");
        assertThat(treatments.get(target(residentId)).active().orElseThrow().id())
                .isEqualTo(wish.id());
    }

    @Test
    void SC_014_currentTreatmentWish_givesTheEmergencyModuleEverythingItNeeds() {
        long residentId = newResident();
        long child = relatedFamilyMember(residentId);
        families.name(child, "Nguyễn Thị Con").phoneNumber(child, "0912345678");
        record(
                residentId,
                new TreatmentCommandBuilder()
                        .hospitals(
                                new PreferredHospital("Bệnh viện X", "12 Lê Lợi"),
                                new PreferredHospital("Bệnh viện Y", "5 Hai Bà Trưng"))
                        .insurance(new HealthInsurance("Trạm y tế P1", "GD4797912345678", LocalDate.of(2027, 6, 30)))
                        .notify(child)
                        .escort(TreatmentEscort.FACILITY_STAFF));

        var view = api.currentTreatmentWish(residentId).orElseThrow();

        assertThat(view.residentId()).isEqualTo(residentId);
        assertThat(view.preferredHospitals())
                .extracting(PreferredHospital::name)
                .containsExactly("Bệnh viện X", "Bệnh viện Y");
        assertThat(view.healthInsurance())
                .hasValueSatisfying(i -> assertThat(i.cardNumber()).isEqualTo("GD4797912345678"));
        assertThat(view.askFamilyFirst()).isTrue();
        assertThat(view.notifyContacts()).singleElement().satisfies(c -> {
            assertThat(c.familyMemberId()).isEqualTo(child);
            assertThat(c.fullName()).isEqualTo("Nguyễn Thị Con");
            assertThat(c.phoneNumber()).isEqualTo("0912345678");
        });
        assertThat(view.accompaniedBy()).isEqualTo(TreatmentEscort.FACILITY_STAFF);
        assertThat(view.at()).isNotNull();
    }

    @Test
    void SC_014_noActiveWish_givesAnEmptyView() {
        assertThat(api.currentTreatmentWish(newResident())).isEmpty();
    }
}
