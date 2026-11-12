package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.internal.closing.service.ClosingEventHandlers;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
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

    /**
     * 011 T026 (FR-042; 005 FR-022): người vắng sau chốt bị bỏ khỏi bữa (phát sinh "−") thì công việc hỗ trợ ăn Chưa đến hạn
     * của chính người đó ở bữa này chuyển Hủy qua {@code TaskSourceApi.sourceCancelled} thật của 005; công việc của người
     * khác cùng bữa giữ nguyên.
     */
    @Test
    void FR_042_absenceAfterClosingDropsMeal_cancelsOnlyThatResidentsMealTask() throws Exception {
        publishedWeek();
        long a = residentIn(room2);
        long b = residentIn(room2);
        clock.setTo(at(LocalTime.of(9, 30)));
        close(dailyMeal(lunch)).andExpect(status().isCreated());
        long taskA = mealTask(a);
        long taskB = mealTask(b);
        clock.setTo(at(LocalTime.of(10, 0)));
        long absence = com.eldercare.backend.support.TestUsers.nextId();
        absences.open(a, new com.eldercare.backend.resident.api.dto.OpenAbsence(absence, at(LocalTime.of(20, 0))));
        setStatus(a, "TEMPORARILY_ABSENT");

        handlers.onAbsenceOpened(
                new com.eldercare.backend.admission.api.events.AbsenceOpened(absence, a, at(LocalTime.of(10, 0))));

        assertThat(taskStatus(taskA)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT closure_rule FROM care_task WHERE id=?", String.class, taskA))
                .isEqualTo("SOURCE_CANCELLED");
        assertThat(taskStatus(taskB)).as("người khác cùng bữa").isEqualTo("NOT_YET_DUE");
    }

    private String taskStatus(long id) {
        return jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, id);
    }

    /** Công việc hỗ trợ ăn Chưa đến hạn căn cứ bữa trưa (ghi thẳng như fixture của 005). */
    private long mealTask(long resident) throws Exception {
        long id = com.eldercare.backend.support.TestUsers.nextId();
        String data = tools.jackson.databind.json.JsonMapper.builder()
                .build()
                .writeValueAsString(new com.eldercare.backend.care.internal.task.domain.CareTaskData(
                        1, "Hỗ trợ ăn", "Hỗ trợ ăn trưa", "EATING", false, null, java.util.Set.of(), null, null, true));
        var planned = java.time.LocalDateTime.ofInstant(at(LocalTime.of(11, 0)), BusinessTime.ZONE);
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,basis_id,shift_id,floor_area_id,planned_at,"
                        + "window_from,window_to,importance,performer_role,status,data,created_by,created_at)"
                        + " VALUES(?,?,1,'MEAL',?,?,?,?,?,?,'NORMAL','CAREGIVER','NOT_YET_DUE',?,?,?)",
                id,
                resident,
                dailyMeal(lunch),
                com.eldercare.backend.support.TestUsers.nextId(),
                floor2,
                planned,
                planned,
                planned.plusMinutes(30),
                data,
                1L,
                java.time.LocalDateTime.ofInstant(at(LocalTime.of(9, 30)), BusinessTime.ZONE));
        return id;
    }
}
