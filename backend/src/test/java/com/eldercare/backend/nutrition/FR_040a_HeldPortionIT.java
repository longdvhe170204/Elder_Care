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
import com.eldercare.backend.admission.api.events.AbsenceClosed;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.nutrition.internal.closing.service.ClosingEventHandlers;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 011 T022 (US4, FR-040a; quickstart Q28; tên {@code FR_040a_SuatGiuIT} trong tasks): suất "dự kiến trở về" của người chưa về
 * tới giờ bữa thành suất giữ, phát sinh "−1" không cần giao; về trong ngưỡng giao trễ thì phục vụ suất giữ, "+1" không cần
 * giao; hết ngưỡng thì ghi hủy bỏ; người về trước giờ bữa thì gỡ dấu, không phát sinh.
 */
class FR_040a_HeldPortionIT extends ClosingTestBase {

    @Autowired
    ClosingEventHandlers handlers;

    @Autowired
    M08JobService jobs;

    long special(long resident) {
        return jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M08.SPECIAL_PORTION' AND resident_id = ? AND status = 'ACTIVE'",
                Long.class,
                resident);
    }

    void asFloorReceiver() {
        doReturn(PermissionResult.allowed(new PermissionBasis(PermissionBasis.Kind.ROLE, java.util.Optional.empty())))
                .when(permissionApi)
                .hasPermission(any(), eq(new PermissionKey(Function.MEAL_RECEIPT, Operation.UPDATE)), any());
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
    }

    @Test
    void FR_040a_notBackAtMealTime_heldPortion_servedWithinThreshold() throws Exception {
        publishedWeek();
        long c = absentResident(room2, at(LocalTime.of(10, 45)));
        long d = absentResident(room2, at(LocalTime.of(10, 50)));
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());

        clock.setTo(at(LocalTime.of(11, 0)));
        jobs.runMinuteTasks(at(LocalTime.of(11, 0)));
        jobs.runMinuteTasks(at(LocalTime.of(11, 1)));

        adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.changeType == 'NOT_RETURNED_AS_EXPECTED')]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.changeType == 'NOT_RETURNED_AS_EXPECTED')].portionCount")
                        .value(hasItem(-1)))
                .andExpect(jsonPath("$[?(@.changeType == 'NOT_RETURNED_AS_EXPECTED')].needsDelivery")
                        .value(hasItem(false)));
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_CONTAINS(content->'$.flags', '\"HELD_PORTION\"') FROM simple_record WHERE id = ?",
                        Integer.class,
                        special(c)))
                .isEqualTo(1);

        asFloorReceiver();
        clock.setTo(at(LocalTime.of(11, 15)));
        String serve = "/api/v1/special-portions/" + special(c) + "/serve-held-portion";
        send(post(serve), "{}").andExpect(status().isOk());
        send(post(serve), "{}").andExpect(status().isConflict());
        String discard = "/api/v1/special-portions/" + special(d) + "/discard-held-portion";
        send(post(discard), "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("HELD_PORTION_STILL_VALID"));
        clock.setTo(at(LocalTime.of(11, 31)));
        send(post(discard), "{}").andExpect(status().isOk());

        adjustmentsOf(dailyMeal(lunch))
                .andExpect(jsonPath("$[?(@.changeType == 'HELD_PORTION_SERVED')].portionCount")
                        .value(hasItem(1)))
                .andExpect(jsonPath("$[?(@.changeType == 'HELD_PORTION_SERVED')].needsDelivery")
                        .value(hasItem(false)));
    }

    @Test
    void FR_040a_backBeforeMealTime_flagCleared_noAdjustment_noHeldPortion() throws Exception {
        publishedWeek();
        long c = absentResident(room2, at(LocalTime.of(10, 45)));
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());

        clock.setTo(at(LocalTime.of(10, 40)));
        setStatus(c, "IN_RESIDENCE");
        absences.close(c);
        handlers.onAbsenceClosed(new AbsenceClosed(TestUsers.nextId(), c, at(LocalTime.of(10, 40))));
        jobs.runMinuteTasks(at(LocalTime.of(11, 0)));

        adjustmentsOf(dailyMeal(lunch)).andExpect(jsonPath("$", hasSize(0)));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind = 'M08.SPECIAL_PORTION' AND resident_id = ?"
                                + " AND status = 'ACTIVE' AND JSON_CONTAINS(content->'$.flags', '\"EXPECTED_RETURN\"')",
                        Integer.class,
                        c))
                .isZero();
    }
}
