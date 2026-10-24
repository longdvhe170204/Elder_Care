package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.error.ReasonRequiredException;
import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.api.events.AllergyExcluded;
import com.cambae.backend.m01.api.events.AllergyRecorded;
import com.cambae.backend.m01.api.events.OtherAllergyRecorded;
import com.cambae.backend.m01.internal.health.domain.HealthItemStatus;
import com.cambae.backend.m01.internal.health.service.HealthItemService;
import com.cambae.backend.m01.internal.health.service.HealthItemService.ItemView;
import com.cambae.backend.m01.internal.health.service.HealthItemService.RecordCommand;
import com.cambae.backend.m01.internal.health.service.HealthItemService.RecordResult;
import com.cambae.backend.m01.internal.health.service.HealthItemService.StatusFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T025 (US2 #1–5; FR-018, FR-018a, FR-019, FR-020; BR-M01-07, BR-M01-11): ghi nhận và loại trừ mục sức khỏe. Dị ứng bắt
 * buộc mức độ; dị ứng loại "khác" nhập tay được đánh dấu không kiểm tra tự động và kèm cảnh báo; chỉ Bác sĩ và Điều dưỡng
 * ghi, loại trừ; loại trừ bắt buộc lý do và một chiều; hồ sơ trạng thái cuối chặn ghi mới. Mỗi lệnh một bản ghi nhật ký.
 */
class BR_M01_07_AllergyIT extends HealthTestBase {

    @Autowired
    HealthItemService items;

    @Autowired
    ApplicationEvents events;

    private RecordResult recordAllergy(long residentId, long allergenId) {
        user.set(doctor.user());
        return items.record(
                target(residentId),
                new RecordCommand(HealthItemType.ALLERGY, allergenId, null, severityId(), sourceId(), false));
    }

    private RecordResult recordCondition(long residentId, String text) {
        user.set(doctor.user());
        return items.record(
                target(residentId),
                new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, text, null, sourceId(), false));
    }

    private int auditCount(String action, long itemId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.HEALTH_ITEM' AND entity_id = ? AND action = ?",
                Integer.class,
                String.valueOf(itemId),
                action);
    }

    @Test
    void FR_018_allergyFromCatalog_isRecordedWithSeveritySourceAndRecorder() {
        long resident = newResident();
        long allergen = allergenId();

        ItemView item = recordAllergy(resident, allergen).item();

        assertThat(item.type()).isEqualTo(HealthItemType.ALLERGY);
        assertThat(item.allergen().id()).isEqualTo(allergen);
        assertThat(item.severity()).isEqualTo("Nhẹ");
        assertThat(item.source()).isEqualTo("Người thân");
        assertThat(item.status()).isEqualTo(HealthItemStatus.ACTIVE);
        assertThat(item.notAutoChecked()).isFalse();
        assertThat(item.pendingCheck()).isTrue();
        assertThat(item.recordedBy()).isEqualTo("#" + doctor.accountId());
        assertThat(item.exclusion()).isNull();
        assertThat(events.stream(AllergyRecorded.class))
                .anyMatch(e -> e.itemId() == item.id() && e.residentId() == resident && e.allergenId() == allergen);
        assertThat(auditCount("HEALTH_ITEM_RECORDED", item.id())).isEqualTo(1);
    }

    @Test
    void BR_M01_11_allergyWithoutSeverity_isRejectedWith422() {
        long resident = newResident();
        user.set(doctor.user());

        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, null, sourceId(), false)))
                .isInstanceOf(InvalidValueException.class);
        assertThat(items.list(target(resident), StatusFilter.ALL)).isEmpty();
    }

    @Test
    void FR_018a_otherAllergy_isFlaggedNotAutoCheckedWithWarning() {
        long resident = newResident();
        user.set(doctor.user());

        RecordResult result = items.record(
                target(resident),
                new RecordCommand(HealthItemType.ALLERGY, null, "Phấn hoa", severityId(), sourceId(), false));

        assertThat(result.item().notAutoChecked()).isTrue();
        assertThat(result.item().pendingCheck()).isFalse();
        assertThat(result.warnings()).extracting("code").containsExactly("ALLERGY_NOT_AUTO_CHECKED");
        assertThat(events.stream(OtherAllergyRecorded.class))
                .anyMatch(e -> e.itemId() == result.item().id());
        assertThat(events.stream(AllergyRecorded.class))
                .noneMatch(e -> e.itemId() == result.item().id());
    }

    @Test
    void FR_018_allergyNeedsExactlyOneOfAllergenOrText_otherTypesNeedTextAndNoAllergen() {
        long resident = newResident();
        user.set(doctor.user());

        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(
                                HealthItemType.ALLERGY, allergenId(), "Phấn hoa", severityId(), sourceId(), false)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.ALLERGY, null, null, severityId(), sourceId(), false)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(
                                HealthItemType.CHRONIC_CONDITION, allergenId(), "Hen", null, sourceId(), false)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.MEDICAL_HISTORY, null, " ", null, sourceId(), false)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_018_inactiveSourceOrUnknownCatalog_isRejected() {
        long resident = newResident();
        user.set(doctor.user());

        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen", null, -1, false)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.ALLERGY, -1L, null, severityId(), sourceId(), false)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_019_onlyDoctorAndNurseRecord_nurseNeedsAssignment() {
        long resident = newResident();
        var caregiver = users.staff(Role.CAREGIVER);
        assignNurse(caregiver, resident);
        user.set(caregiver.user());

        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen", null, sourceId(), false)))
                .isInstanceOf(PermissionDeniedException.class);

        user.set(nurse.user());
        assertThatThrownBy(() -> items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen", null, sourceId(), false)))
                .isInstanceOf(NotFoundException.class);

        assignNurse(nurse, resident);
        assertThat(items.record(
                                target(resident),
                                new RecordCommand(
                                        HealthItemType.CHRONIC_CONDITION, null, "Hen", null, sourceId(), false))
                        .item()
                        .recordedBy())
                .isEqualTo("#" + nurse.accountId());
    }

    @Test
    void FR_019_exclude_needsReason_isOneWay_andKeepsHistory() {
        long resident = newResident();
        ItemView item = recordAllergy(resident, allergenId()).item();

        assertThatThrownBy(() -> items.exclude(target(resident), item.id(), null))
                .isInstanceOf(ReasonRequiredException.class);

        ItemView excluded = items.exclude(target(resident), item.id(), new Reason("Ghi nhầm người"));

        assertThat(excluded.status()).isEqualTo(HealthItemStatus.EXCLUDED);
        assertThat(excluded.exclusion().reason()).isEqualTo("Ghi nhầm người");
        assertThat(excluded.exclusion().by()).isEqualTo("#" + doctor.accountId());
        assertThat(excluded.exclusion().at()).isNotNull();
        assertThat(excluded.freeText()).isEqualTo(item.freeText());
        assertThat(events.stream(AllergyExcluded.class)).anyMatch(e -> e.itemId() == item.id());
        assertThat(auditCount("HEALTH_ITEM_EXCLUDED", item.id())).isEqualTo(1);
        assertThatThrownBy(() -> items.exclude(target(resident), item.id(), new Reason("Lần hai")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("ALREADY_EXCLUDED"));
    }

    @Test
    void FR_019_excludeThenRecordAgain_isAllowed_andListFiltersByStatus() {
        long resident = newResident();
        long allergen = allergenId();
        ItemView wrong = recordAllergy(resident, allergen).item();
        items.exclude(target(resident), wrong.id(), new Reason("Nhầm dị nguyên"));

        ItemView again = recordAllergy(resident, allergen).item();

        assertThat(ids(items.list(target(resident), StatusFilter.ACTIVE))).containsExactly(again.id());
        assertThat(ids(items.list(target(resident), StatusFilter.EXCLUDED))).containsExactly(wrong.id());
        assertThat(ids(items.list(target(resident), StatusFilter.ALL))).containsExactly(again.id(), wrong.id());
    }

    @Test
    void BR_M01_05_closedProfile_blocksRecordAndExclude() {
        long resident = newResident();
        ItemView item = recordCondition(resident, "Tăng huyết áp").item();
        setStatus(resident, "RESIDENCY_ENDED");

        assertThatThrownBy(() -> recordCondition(resident, "Gout"))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("PROFILE_CLOSED"));
        assertThatThrownBy(() -> items.exclude(target(resident), item.id(), new Reason("Nhầm")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("PROFILE_CLOSED"));
    }

    @Test
    void FR_019_excludeOfAnotherResidentsItem_isNotFound() {
        long resident = newResident();
        long other = newResident();
        ItemView item = recordCondition(resident, "Viêm khớp").item();
        user.set(doctor.user());

        assertThatThrownBy(() -> items.exclude(target(other), item.id(), new Reason("Nhầm")))
                .isInstanceOf(NotFoundException.class);
    }

    private static List<Long> ids(List<ItemView> views) {
        return views.stream().map(ItemView::id).toList();
    }
}
