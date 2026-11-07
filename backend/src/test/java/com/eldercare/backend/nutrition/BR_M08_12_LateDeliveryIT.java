package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.nutrition.api.events.MealSlipDeliveredLate;
import com.eldercare.backend.nutrition.api.events.MealSlipLateNotice;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T028 (US5, FR-048a, BR-M08-12; quickstart Q36; tên {@code BR_M08_12_GiaoTreIT} trong tasks): quá giờ bữa + ngưỡng
 * giao trễ CFG-M08-04 mà phiếu chưa giao thì Trưởng tầng của tầng/khu và bếp được báo mức Nhẹ một lần; phiếu đã giao
 * không báo; lần chốt sau giờ bữa không dùng thông báo này. Giao sau ngưỡng phát sự kiện giao trễ cho 016.
 */
class BR_M08_12_LateDeliveryIT extends SlipTestBase {

    /** Phiếu của test: dữ liệu sót của lần chạy khác có thể trùng ngày bữa. */
    java.util.Set<Long> mine = java.util.Set.of();

    @Test
    void BR_M08_12_undeliveredSlipPastThreshold_floorManagerAndKitchenNotifiedOnce() throws Exception {
        publishedWeek();
        residentIn(room2);
        residentIn(room3);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long slip2 = slipId(dailyMeal(lunch), floor2);
        long slip3 = slipId(dailyMeal(lunch), floor3);
        mine = java.util.Set.of(slip2, slip3);
        clock.setTo(at(LocalTime.of(10, 50)));
        prepareAndDeliver(dailyMeal(lunch), slip2, List.of());

        jobs.runMinuteTasks(at(LocalTime.of(11, 29)));
        assertThat(events.stream(MealSlipLateNotice.class).filter(n -> mine.contains(n.slipId())))
                .isEmpty();

        jobs.runMinuteTasks(at(LocalTime.of(11, 30)));
        jobs.runMinuteTasks(at(LocalTime.of(11, 31)));
        assertThat(events.stream(MealSlipLateNotice.class).filter(n -> mine.contains(n.slipId())))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.slipId()).isEqualTo(slip3);
                    var request = n.notificationRequests().getFirst();
                    assertThat(request.level()).contains(NotificationLevel.LOW);
                    assertThat(request.recipientGroups())
                            .anySatisfy(r -> assertThat(r)
                                    .isInstanceOfSatisfying(
                                            RecipientGroup.FloorManager.class,
                                            m -> assertThat(m.floorAreaId()).isEqualTo(floor3)))
                            .anySatisfy(r -> assertThat(r).isInstanceOf(RecipientGroup.FacilityRole.class));
                });

        clock.setTo(at(LocalTime.of(11, 40)));
        prepareAndDeliver(dailyMeal(lunch), slip3, List.of());
        assertThat(events.stream(MealSlipDeliveredLate.class))
                .singleElement()
                .satisfies(e -> assertThat(e.slipId()).isEqualTo(slip3));
    }

    @Test
    void BR_M08_12_closedAfterMealTime_noLateNotice() throws Exception {
        publishedWeek();
        residentIn(room2);
        clock.setTo(at(LocalTime.of(11, 10)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        mine = java.util.Set.of(slipId(dailyMeal(lunch), floor2));

        jobs.runMinuteTasks(at(LocalTime.of(11, 45)));
        assertThat(events.stream(MealSlipLateNotice.class).filter(n -> mine.contains(n.slipId())))
                .isEmpty();
    }
}
