package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import com.eldercare.backend.resident.internal.health.domain.HealthItemStatus;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.ItemView;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T025 (US2 #1–5; FR-018, FR-018a, FR-019, FR-020; BR-M01-07, BR-M01-11): ghi nhận và loại trừ mục sức khỏe. Dị ứng bắt
 * buộc mức độ; dị ứng loại "khác" nhập tay được đánh dấu không kiểm tra tự động và kèm cảnh báo; chỉ Bác sĩ và Điều dưỡng
 * ghi, loại trừ; loại trừ bắt buộc lý do và một chiều; hồ sơ trạng thái cuối chặn ghi mới. Mỗi lệnh một bản ghi nhật ký.
 */
class BR_03_AllergyIT extends HealthTestBase {

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

    private static List<Long> ids(List<ItemView> views) {
        return views.stream().map(ItemView::id).toList();
    }
}
