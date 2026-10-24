package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.HealthItemApi;
import com.cambae.backend.m01.api.dto.HealthItemCheckSource;
import com.cambae.backend.m01.api.dto.HealthItemType;
import com.cambae.backend.m01.api.dto.HealthItemView;
import com.cambae.backend.m01.api.events.AllergyRecorded;
import com.cambae.backend.m01.internal.health.service.AllergyCheckRetry;
import com.cambae.backend.m01.internal.health.service.HealthItemService;
import com.cambae.backend.m01.internal.health.service.HealthItemService.ItemView;
import com.cambae.backend.m01.internal.health.service.HealthItemService.RecordCommand;
import java.time.Duration;
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
@Import(BR_M01_12_PendingCheckIT.FailingChecker.class)
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

    @Test
    void BR_M01_12_flagStaysUntilBothModulesCheck_markCheckedIsIdempotent() {
        long resident = newResident();
        ItemView item = recordAllergy(resident);

        api.markChecked(item.id(), HealthItemCheckSource.MEDICATION);
        assertThat(pending(resident)).isTrue();

        api.markChecked(item.id(), HealthItemCheckSource.MEDICATION);
        assertThat(pending(resident)).isTrue();

        api.markChecked(item.id(), HealthItemCheckSource.NUTRITION);
        assertThat(pending(resident)).isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.HEALTH_ITEM' AND entity_id = ?"
                                + " AND action = 'HEALTH_ITEM_CHECKED'",
                        Integer.class,
                        String.valueOf(item.id())))
                .isEqualTo(3);
    }

    @Test
    void BR_M01_12_retryJob_republishesOnlyAfter15Minutes_andStopsOnceChecked() {
        long resident = newResident();
        ItemView item = recordAllergy(resident);
        long once = republished(item.id());

        retry.run();
        assertThat(republished(item.id())).as("chưa quá 15 phút").isEqualTo(once);

        clock.advanceBy(Duration.ofMinutes(16));
        assertThat(retry.run()).isGreaterThanOrEqualTo(1);
        assertThat(republished(item.id())).isEqualTo(once + 1);

        api.markChecked(item.id(), HealthItemCheckSource.MEDICATION);
        retry.run();
        assertThat(republished(item.id())).as("còn cờ dinh dưỡng").isEqualTo(once + 2);

        api.markChecked(item.id(), HealthItemCheckSource.NUTRITION);
        retry.run();
        assertThat(republished(item.id())).as("hết cờ thì dừng").isEqualTo(once + 2);
    }

    @Test
    void BR_M01_12_retryJob_skipsExcludedItemsAndOtherAllergies() {
        long resident = newResident();
        ItemView excluded = recordAllergy(resident);
        items.exclude(target(resident), excluded.id(), new com.cambae.backend.common.Reason("Ghi nhầm"));
        user.set(doctor.user());
        ItemView other = items.record(
                        target(resident),
                        new RecordCommand(HealthItemType.ALLERGY, null, "Phấn hoa", severityId(), sourceId(), false))
                .item();
        long excludedBefore = republished(excluded.id());

        clock.advanceBy(Duration.ofMinutes(16));
        retry.run();

        assertThat(republished(excluded.id())).isEqualTo(excludedBefore);
        assertThat(republished(other.id())).isZero();
    }

    private boolean pending(long residentId) {
        return api.active(residentId).getFirst().pendingCheck();
    }
}
