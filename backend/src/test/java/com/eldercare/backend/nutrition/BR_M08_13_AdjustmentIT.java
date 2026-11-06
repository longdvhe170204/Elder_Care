package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.billing.api.events.CostSourceChanged;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.nutrition.api.dto.StayMealView;
import com.eldercare.backend.nutrition.api.events.AdjustmentsPending;
import com.eldercare.backend.nutrition.api.events.StayMealChanged;
import com.eldercare.backend.nutrition.internal.closing.service.ClosingEventHandlers;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import com.eldercare.backend.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 011 T022 (US4, FR-039 → FR-041, FR-043, BR-M08-13; quickstart Q23, Q25 → Q27; tên {@code BR_M08_13_PhatSinhIT} trong
 * tasks): thay đổi sau chốt chưa tới giờ bữa tạo phát sinh theo tầng/khu và chế độ ăn, chạy lại không trùng (SC-002); bếp
 * xác nhận, tầng chưa có phiếu thì lập phiếu khi xác nhận "+"; nhắc tại CFG-M08-05; người thân kết thúc sớm hủy suất ăn người
 * thân và báo 010; đổi món sau chốt; Bác sĩ chỉ định món riêng; suất bổ sung sau giờ bữa trong ngưỡng giao trễ. Listener
 * sau commit gọi {@link ClosingEventHandlers} (test gọi thẳng trong transaction của test).
 */
class BR_M08_13_AdjustmentIT extends ClosingTestBase {

    @Autowired
    ClosingEventHandlers handlers;

    @Autowired
    M08JobService jobs;

    @Test
    void FR_039_bedTransferAfterClosing_minusOldFloor_plusNewFloor_slipCreatedOnConfirm() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        int slipsBefore = slipCount(dailyMeal(lunch));
        clock.setTo(at(LocalTime.of(10, 0)));
        locations.closeStays(a, at(LocalTime.of(10, 0)));
        locations.stay(a, room3, at(LocalTime.of(10, 0)), null);

        BedTransferred moved = new BedTransferred(a, 1, 2, floor2, floor3, at(LocalTime.of(10, 0)));
        handlers.onBedTransferred(moved);
        handlers.onBedTransferred(moved);

        String body = adjustmentsOf(dailyMeal(lunch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.floorAreaId == " + floor2 + ")].portionCount")
                        .value(hasItem(-1)))
                .andExpect(jsonPath("$[?(@.floorAreaId == " + floor3 + ")].portionCount")
                        .value(hasItem(1)))
                .andExpect(jsonPath("$[*].needsDelivery", hasItem(true)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Integer> plus = JsonPath.read(body, "$[?(@.floorAreaId == " + floor3 + ")].id");
        long plusId = plus.getFirst();

        f.asDoctor();
        send(post("/api/v1/adjustments/" + plusId + "/confirm"), "{}").andExpect(status().isForbidden());
        f.asKitchen();
        send(post("/api/v1/adjustments/" + plusId + "/confirm"), "{}").andExpect(status().isOk());
        send(post("/api/v1/adjustments/" + plusId + "/confirm"), "{}").andExpect(status().isConflict());
        assertThat(slipCount(dailyMeal(lunch))).isEqualTo(slipsBefore + 1);
        adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.id == " + plusId + ")].kitchenConfirmedAt")
                        .value(hasItem(org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void FR_043_familyStayEndsBeforeMeal_minusOne_familyMealCancelled_billingNotified() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        long stay = TestUsers.nextId();
        long familyMember = TestUsers.nextId();
        stays.stay(new StayMealView(stay, a, familyMember, at(LocalTime.MIDNIGHT), at(LocalTime.of(23, 0)), true));
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        clock.setTo(at(LocalTime.of(10, 0)));
        stays.end(stay, at(LocalTime.of(10, 0)));

        handlers.onStayMealChanged(new StayMealChanged(
                stay,
                a,
                familyMember,
                "ENDED",
                true,
                at(LocalTime.MIDNIGHT),
                at(LocalTime.of(10, 0)),
                "stay-end-" + stay));

        adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].portionCount").value(-1))
                .andExpect(jsonPath("$[0].changeType").value("FAMILY_STAY"));
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM simple_record WHERE kind = 'M08.FAMILY_MEAL' AND resident_id = ?",
                        String.class,
                        a))
                .isEqualTo("CANCELLED");
        assertThat(events.stream(CostSourceChanged.class))
                .singleElement()
                .satisfies(e -> assertThat(e.cancelled()).isTrue());
    }

    @Test
    void FR_041_unconfirmedAdjustmentAtReminderLead_remindsKitchenAndDoctorOnce() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        clock.setTo(at(LocalTime.of(10, 0)));
        setStatus(a, "HOSPITALIZED");
        handlers.onResidentStatusChanged(new com.eldercare.backend.resident.api.events.ResidentStatusChanged(
                a,
                com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE,
                com.eldercare.backend.resident.api.dto.ResidentStatus.HOSPITALIZED,
                null,
                TestUsers.nextId(),
                at(LocalTime.of(10, 0))));
        assertThat(events.stream(AdjustmentsPending.class))
                .singleElement()
                .satisfies(e -> assertThat(e.reminder()).isFalse());

        jobs.runMinuteTasks(at(LocalTime.of(10, 29)));
        jobs.runMinuteTasks(at(LocalTime.of(10, 30)));
        jobs.runMinuteTasks(at(LocalTime.of(10, 31)));

        assertThat(events.stream(AdjustmentsPending.class).filter(AdjustmentsPending::reminder))
                .singleElement()
                .satisfies(e -> assertThat(e.notificationRequests())
                        .flatExtracting(r -> r.recipientGroups())
                        .contains(new com.eldercare.backend.common.notification.RecipientGroup.FacilityRole(
                                Role.DOCTOR)));
    }

    @Test
    void FR_040_extraPortion_afterMealTimeWithinThreshold_only() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        doReturn(PermissionResult.allowed(new PermissionBasis(PermissionBasis.Kind.ROLE, java.util.Optional.empty())))
                .when(permissionApi)
                .hasPermission(any(), eq(new PermissionKey(Function.MEAL_RECEIPT, Operation.CREATE)), any());
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
        String body = "{\"residentId\":" + a + ",\"floorAreaId\":" + floor2 + ",\"reason\":\"Về muộn\"}";
        String url = DAILY_MEALS + dailyMeal(lunch) + "/extra-portions";

        clock.setTo(at(LocalTime.of(10, 50)));
        send(post(url), body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("BEFORE_MEAL_TIME"));
        clock.setTo(at(LocalTime.of(11, 20)));
        send(post(url), body).andExpect(status().isCreated());
        clock.setTo(at(LocalTime.of(11, 40)));
        send(post(url), body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("PAST_LATE_DELIVERY_THRESHOLD"));
        send(post(url), "{\"residentId\":" + a + ",\"floorAreaId\":" + floor2 + "}")
                .andExpect(status().isUnprocessableContent());

        adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.changeType == 'EXTRA_PORTION_REQUEST')].portionCount")
                        .value(hasItem(1)));
        assertThat(events.stream(AdjustmentsPending.class)).hasSize(1);
    }

    @Test
    void FR_039_dishSwapAfterClosing_isAdjustmentPerFloor_beforeMealTime() throws Exception {
        long menu = publishedWeek();
        residentIn(room2);
        long fish = f.activeDish(List.of(), List.of(defaultDiet));
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        clock.setTo(at(LocalTime.of(10, 0)));

        f.asKitchen();
        send(
                        post(MENUS + "/" + menu + "/dish-swaps"),
                        "{\"date\":\"" + day + "\",\"mealId\":" + lunch + ",\"dietId\":" + defaultDiet
                                + ",\"oldDishId\":"
                                + plain + ",\"newDishId\":" + fish + ",\"reason\":\"Hết thịt\",\"confirmedWarnings\":"
                                + strings(codes(menu)) + "}")
                .andExpect(status().isOk());

        adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.changeType == 'DISH_SWAP' && @.floorAreaId == " + floor2 + ")]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.changeType == 'DISH_SWAP')].portionCount")
                        .value(hasItem(0)));
    }

    @Test
    void FR_037_doctorAssignsDish_toSpecialPortion_createsAdjustment() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        f.activeAssignment(a, defaultDiet, "PUREED", List.of(), time.now());
        long soup = f.activeDish(List.of(), List.of(defaultDiet));
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialPortions").value(1));
        long special = jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M08.SPECIAL_PORTION' AND resident_id = ?", Long.class, a);
        String url = "/api/v1/special-portions/" + special + "/assign-dish";

        f.asKitchen();
        send(post(url), "{\"dishId\":" + soup + "}").andExpect(status().isForbidden());
        f.asDoctor();
        send(post(url), "{\"dishId\":" + soup + ",\"note\":\"Dễ nuốt\"}").andExpect(status().isOk());

        adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.changeType == 'PORTION_CHANGED')].portionCount")
                        .value(hasItem(0)));
        assertThat(jdbc.queryForObject(
                        "SELECT content->>'$.assignedDishId' FROM simple_record WHERE kind = 'M08.SPECIAL_PORTION'"
                                + " AND resident_id = ? AND status = 'ACTIVE'",
                        Long.class,
                        a))
                .isEqualTo(soup);
    }
}
