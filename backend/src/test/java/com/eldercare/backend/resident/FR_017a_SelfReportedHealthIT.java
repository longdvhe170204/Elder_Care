package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecordContent;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.ItemView;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.StatusFilter;
import com.eldercare.backend.resident.internal.health.service.InitialHealthRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T101 → T103 (Q-400, BR-M01-17, FR-017a): Hành chính ghi tự khai của gia đình khi người cao tuổi còn Đang tiếp nhận.
 * Mục có hiệu lực ngay nhưng mang dấu "chưa kiểm chứng"; Hành chính chỉ thấy phần mình ghi, không loại trừ, không xác nhận;
 * Bác sĩ hoặc Điều dưỡng kiểm chứng.
 */
class FR_017a_SelfReportedHealthIT extends HealthTestBase {

    @Autowired
    HealthItemService items;

    @Autowired
    InitialHealthRecordService records;

    @Autowired
    HealthItemApi itemApi;

    @Autowired
    com.eldercare.backend.resident.internal.wish.service.TreatmentWishService treatmentWishes;

    @Autowired
    ApplicationEvents events;

    private long admitting() {
        long resident = newResident();
        setStatus(resident, "ADMISSION_IN_PROGRESS");
        return resident;
    }

    private ItemView byOffice(long resident, String text) {
        user.set(office.user());
        return items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, text, null, sourceId(), false))
                .item();
    }

    private static final InitialHealthRecordContent CONTENT =
            new InitialHealthRecordContent("O+", "Đi lại khó", null, null, null, null, "Thuốc A", null);

    @Test
    void FR_017a_officeRecordsItem_activeImmediately_butUnverified() {
        long resident = admitting();

        ItemView item = byOffice(resident, "Tăng huyết áp");

        assertThat(item.unverified()).isTrue();
        assertThat(item.verifiedBy()).isNull();
        assertThat(itemApi.active(resident)).extracting("id").contains(item.id());
    }

    @Test
    void FR_017a_officeAllergy_publishesAllergyRecorded_soMedicationAndNutritionStillCheck() {
        long resident = admitting();
        long allergen = allergenId();
        user.set(office.user());

        ItemView item = items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.ALLERGY, allergen, null, severityId(), sourceId(), false))
                .item();

        assertThat(item.unverified()).isTrue();
        assertThat(item.pendingCheck()).isTrue();
        assertThat(events.stream(AllergyRecorded.class))
                .anySatisfy(e -> assertThat(e.itemId()).isEqualTo(item.id()));
    }

    @Test
    void FR_017a_officeCannotRecord_afterResidentIsInResidence() {
        long resident = newResident();
        user.set(office.user());

        Throwable thrown = catchThrowable(() -> items.record(
                target(resident),
                new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen", null, sourceId(), false)));

        assertThat(thrown).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_017a_doctorRecordedItem_isVerified() {
        long resident = admitting();
        user.set(doctor.user());

        ItemView item = items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen", null, sourceId(), false))
                .item();

        assertThat(item.unverified()).isFalse();
    }

    @Test
    void FR_017a_officeSeesOnlyItsOwnUnverifiedItems() {
        long resident = admitting();
        ItemView own = byOffice(resident, "Tiểu đường");
        user.set(doctor.user());
        ItemView byDoctor = items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Gout", null, sourceId(), false))
                .item();

        user.set(office.user());
        assertThat(items.list(target(resident), StatusFilter.ALL))
                .extracting(ItemView::id)
                .containsExactly(own.id());

        user.set(doctor.user());
        assertThat(items.list(target(resident), StatusFilter.ALL))
                .extracting(ItemView::id)
                .containsExactlyInAnyOrder(own.id(), byDoctor.id());
    }

    @Test
    void FR_017a_officeCannotExclude_evenItsOwnItem() {
        long resident = admitting();
        ItemView own = byOffice(resident, "Viêm khớp");
        user.set(office.user());

        Throwable thrown =
                catchThrowable(() -> items.exclude(target(resident), own.id(), new Reason("Nhập nhầm do gia đình")));

        assertThat(thrown).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_017a_doctorVerifies_thenOfficeNoLongerSeesTheItem() {
        long resident = admitting();
        ItemView own = byOffice(resident, "Loãng xương");
        user.set(doctor.user());
        String doctorName = items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen", null, sourceId(), false))
                .item()
                .recordedBy();

        ItemView verified = items.verify(target(resident), own.id());

        assertThat(verified.unverified()).isFalse();
        assertThat(verified.verifiedBy()).isEqualTo(doctorName);
        assertThat(verified.verifiedAt()).isNotNull();
        user.set(office.user());
        assertThat(items.list(target(resident), StatusFilter.ALL)).isEmpty();
        user.set(doctor.user());
        Throwable again = catchThrowable(() -> items.verify(target(resident), own.id()));
        assertThat(again)
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("ALREADY_VERIFIED"));
    }

    @Test
    void FR_017a_nurseVerifies_officeCannot() {
        long resident = admitting();
        assignNurse(nurse, resident);
        ItemView own = byOffice(resident, "Suy tim");

        user.set(office.user());
        assertThat(catchThrowable(() -> items.verify(target(resident), own.id())))
                .isInstanceOf(PermissionDeniedException.class);

        user.set(nurse.user());
        assertThat(items.verify(target(resident), own.id()).unverified()).isFalse();
    }

    @Test
    void FR_017a_officeDraftsInitialRecord_butCannotConfirm_andOnlySeesOwnDraft() {
        long resident = admitting();
        user.set(office.user());

        var draft = records.saveDraft(target(resident), null, CONTENT);

        assertThat(draft.confirmed()).isFalse();
        assertThat(records.get(target(resident))).isPresent();
        assertThat(catchThrowable(() -> records.confirm(target(resident), "\"" + draft.version() + "\"")))
                .isInstanceOf(PermissionDeniedException.class);

        user.set(doctor.user());
        var confirmed = records.confirm(target(resident), "\"" + draft.version() + "\"");
        assertThat(confirmed.confirmed()).isTrue();

        user.set(office.user());
        assertThat(catchThrowable(() -> records.get(target(resident)).orElseThrow(NotFoundException::new)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR_017a_officeCannotEditDraftStartedByDoctor() {
        long resident = admitting();
        user.set(doctor.user());
        var draft = records.saveDraft(target(resident), null, CONTENT);

        user.set(office.user());
        assertThat(records.get(target(resident))).isEmpty();
        assertThat(catchThrowable(() -> records.saveDraft(target(resident), "\"" + draft.version() + "\"", CONTENT)))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_017a_officeStillCannotReadOrRecordTreatmentWish() {
        long resident = admitting();
        user.set(office.user());

        assertThat(catchThrowable(() -> treatmentWishes.get(target(resident))))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
