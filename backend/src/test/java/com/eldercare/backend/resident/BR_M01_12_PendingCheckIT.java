package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.HealthItemView;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import com.eldercare.backend.resident.internal.health.service.AllergyCheckRetry;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.ItemView;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T025 (US2 #6–8; FR-020; BR-M01-12; research H-07, H-12): kiểm tra dị ứng tách khỏi transaction ghi mục. Mục luôn
 * được lưu dù listener của 006, 011 lỗi; cờ chờ kiểm tra còn cho tới khi cả hai module gọi {@code markChecked}; job phát
 * lại {@code AllergyRecorded} cho mục còn cờ quá 15 phút và dừng khi hết cờ.
 */
@Import({BR_M01_12_PendingCheckIT.FailingChecker.class, AllergyCheckersOff.class})
class BR_M01_12_PendingCheckIT extends HealthTestBase {

    /** Giả lập 006 hoặc 011 lỗi khi kiểm tra: listener ném ngoại lệ, lệnh ghi mục không bị ảnh hưởng. */
    @TestConfiguration
    static class FailingChecker {

        @ApplicationModuleListener
        void on(AllergyRecorded event) {
            throw new IllegalStateException("006, 011 chưa chạy được");
        }
    }

    @Autowired
    HealthItemService items;

    @Autowired
    HealthItemApi api;

    @Autowired
    AllergyCheckRetry retry;

    @Autowired
    TestClockControl clock;

    @Autowired
    ApplicationEvents events;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    private ItemView recordAllergy(long residentId) {
        user.set(doctor.user());
        return items.record(
                        target(residentId),
                        new RecordCommand(HealthItemType.ALLERGY, allergenId(), null, severityId(), sourceId(), false))
                .item();
    }

    private long republished(long itemId) {
        return events.stream(AllergyRecorded.class)
                .filter(e -> e.itemId() == itemId)
                .count();
    }

    @Test
    void BR_M01_12_checkerFailing_itemIsStillSavedAndFlagged() {
        long resident = newResident();

        ItemView item = recordAllergy(resident);

        HealthItemView saved = api.active(resident, HealthItemType.ALLERGY).getFirst();
        assertThat(saved.id()).isEqualTo(item.id());
        assertThat(saved.pendingCheck()).isTrue();
        assertThat(saved.allergenId()).isPresent();
        assertThat(saved.notAutoChecked()).isFalse();
    }

    private List<Boolean> flags(long itemId) {
        return jdbc.queryForObject(
                "SELECT pending_medication_check, pending_nutrition_check FROM health_item WHERE id = ?",
                (rs, i) -> List.of(rs.getBoolean(1), rs.getBoolean(2)),
                itemId);
    }

    private boolean pending(long residentId) {
        return api.active(residentId).getFirst().pendingCheck();
    }
}
