package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.internal.health.service.HealthItemService;
import com.cambae.backend.m01.internal.health.service.HealthItemService.ItemView;
import com.cambae.backend.m01.internal.health.service.HealthItemService.RecordCommand;
import com.cambae.backend.m01.internal.health.service.HealthItemService.StatusFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T025 (FR-021): ghi mục trùng một mục đang Hiệu lực thì 409 {@code DUPLICATE_HEALTH_ITEM} kèm mã mục đã có; gửi lại
 * với {@code confirmDuplicate} thì ghi. Trùng theo dị nguyên, hoặc cùng loại và cùng nội dung (không phân biệt hoa
 * thường, bỏ khoảng trắng thừa, có phân biệt dấu). Mục đã loại trừ không tính là trùng.
 */
class HealthItemDuplicateIT extends HealthTestBase {

    @Autowired
    HealthItemService items;

    private ItemView condition(long residentId, String text, boolean confirmDuplicate) {
        user.set(doctor.user());
        return items.record(
                        target(residentId),
                        new RecordCommand(
                                HealthItemType.CHRONIC_CONDITION, null, text, null, sourceId(), confirmDuplicate))
                .item();
    }

    private ItemView allergy(long residentId, long allergenId, boolean confirmDuplicate) {
        user.set(doctor.user());
        return items.record(
                        target(residentId),
                        new RecordCommand(
                                HealthItemType.ALLERGY, allergenId, null, severityId(), sourceId(), confirmDuplicate))
                .item();
    }

    private static void assertDuplicateOf(Throwable thrown, long existingId) {
        assertThat(thrown).isInstanceOfSatisfying(BusinessException.class, e -> {
            assertThat(e.errorCode()).isEqualTo("DUPLICATE_HEALTH_ITEM");
            assertThat(e.status().value()).isEqualTo(409);
            assertThat(e.properties()).containsEntry("existingItemId", existingId);
        });
    }

    @Test
    void FR_021_sameAllergen_isDuplicate_untilConfirmed() {
        long resident = newResident();
        long allergen = allergenId();
        ItemView first = allergy(resident, allergen, false);

        assertDuplicateOf(catchThrowable(() -> allergy(resident, allergen, false)), first.id());
        ItemView second = allergy(resident, allergen, true);

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(items.list(target(resident), StatusFilter.ACTIVE)).hasSize(2);
    }

    @Test
    void FR_021_sameTypeAndText_ignoresCaseAndExtraSpaces_butKeepsDiacritics() {
        long resident = newResident();
        ItemView first = condition(resident, "Tăng huyết áp", false);

        assertDuplicateOf(catchThrowable(() -> condition(resident, "  tăng   HUYẾT áp ", false)), first.id());
        assertThat(condition(resident, "Tang huyet ap", false).id()).isNotEqualTo(first.id());
    }

    @Test
    void FR_021_sameTextButDifferentType_isNotDuplicate() {
        long resident = newResident();
        condition(resident, "Hen suyễn", false);
        user.set(doctor.user());

        assertThat(items.record(
                                target(resident),
                                new RecordCommand(
                                        HealthItemType.MEDICAL_HISTORY, null, "Hen suyễn", null, sourceId(), false))
                        .item()
                        .type())
                .isEqualTo(HealthItemType.MEDICAL_HISTORY);
    }

    @Test
    void FR_021_excludedItem_doesNotCount() {
        long resident = newResident();
        ItemView first = condition(resident, "Đái tháo đường", false);
        items.exclude(target(resident), first.id(), new Reason("Ghi nhầm người"));

        assertThat(condition(resident, "Đái tháo đường", false).id()).isNotEqualTo(first.id());
    }

    @Test
    void FR_021_sameAllergenOfAnotherResident_isNotDuplicate() {
        long allergen = allergenId();
        allergy(newResident(), allergen, false);

        assertThat(allergy(newResident(), allergen, false).id()).isPositive();
    }
}
