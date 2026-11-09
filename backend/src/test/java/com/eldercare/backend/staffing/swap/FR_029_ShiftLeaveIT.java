package com.eldercare.backend.staffing.swap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.swap.service.LeaveService;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** FR-026 → FR-031, FR-026a, FR-045: nghỉ đột xuất theo ca và theo ngày, duyệt một phần, lập thay, tách ca. */
class FR_029_ShiftLeaveIT extends SwapTestBase {

    @Autowired
    LeaveService leaveService;

    @Autowired
    com.eldercare.backend.staffing.scheduling.M09MinuteJob minuteJob;

    @Autowired
    org.springframework.context.ApplicationEventPublisher events;

    @Autowired
    org.springframework.transaction.support.TransactionTemplate transactions;

    private String statusOf(long requestId) {
        return jdbc.queryForObject("SELECT status FROM approval_request WHERE id=?", String.class, requestId);
    }

    private long createByShift(TestUsers.TestUser by, TestUsers.TestUser staff, long... shiftIds) throws Exception {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("kind", "BY_SHIFT");
        if (staff != null) {
            body.put("staffId", staff.staffId());
        }
        body.put("shiftIds", java.util.Arrays.stream(shiftIds).boxed().toList());
        body.put("reason", "[Lý do nghỉ riêng tư]");
        var response = send("/api/v1/leave-requests", by, body).andReturn().getResponse();
        if (response.getStatus() != 201) {
            throw new AssertionError(
                    "Lập nghỉ thất bại: " + response.getStatus() + " " + response.getContentAsString());
        }
        return json.readTree(response.getContentAsString())
                .get("requestIds")
                .get(0)
                .asLong();
    }

    private boolean onLeave(long shiftId, TestUsers.TestUser user) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM shift_staff WHERE shift_id=? AND staff_id=? AND approved_leave_request_id IS NOT NULL",
                Integer.class,
                shiftId,
                user.staffId());
        return n != null && n > 0;
    }

    @Test
    void FR_029_approvedLeaveMarksStaffOnLeaveAndKeepsOtherShifts() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long other = shift(base.plus(Duration.ofDays(1)).plus(Duration.ofHours(7)), 8, null, giver);
        long id = createByShift(giver, null, x);
        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
        send("/api/v1/leave-requests/" + id + "/approve", head, null).andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("APPLIED");
        assertThat(onLeave(x, giver)).isTrue();
        assertThat(onLeave(other, giver)).isFalse();
        // Khóa đã nhả: xin nghỉ lại ca khác không bị chặn bởi ca đã quyết định.
        createByShift(giver, null, other);
    }

    @Test
    void FR_041_approvedLeaveRemovesTripDutyOnlyForApprovedShifts() throws Exception {
        long x1 = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long x2 = shift(base.plus(Duration.ofDays(1)).plus(Duration.ofHours(7)), 8, null, giver);
        long id = createByShift(giver, null, x1, x2);
        var items = List.of(Map.of("shiftId", x1, "result", "APPROVED"), Map.of("shiftId", x2, "result", "REJECTED"));
        send("/api/v1/leave-requests/" + id + "/approve", head, Map.of("items", items, "reason", "[Thiếu người]"))
                .andExpect(status().isOk());
        org.mockito.Mockito.verify(tripDuties)
                .removeDuty(
                        org.mockito.ArgumentMatchers.eq(giver.staffId()),
                        org.mockito.ArgumentMatchers.eq(x1),
                        org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verifyNoMoreInteractions(tripDuties);
    }

    @Test
    void FR_026a_publishingTheMonthTurnsPendingDayLeaveIntoLeaveForThatDaysShifts() throws Exception {
        Instant future = base.plus(Duration.ofDays(75));
        YearMonth month = YearMonth.from(future.atZone(com.eldercare.backend.common.time.BusinessTime.ZONE));
        long draftSchedule = insertRow(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FLOOR',?,'DRAFT')",
                month.toString(),
                floor);
        LocalDate day = month.atDay(12);
        Instant start = time.startOfDay(day).plus(Duration.ofHours(7));
        long sameDay = insertRow(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,FALSE,'DRAFT',NULL)",
                draftSchedule,
                templateId,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plus(Duration.ofHours(8))),
                floor);
        Instant next = time.startOfDay(day.plusDays(1)).plus(Duration.ofHours(7));
        long nextDay = insertRow(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,FALSE,'DRAFT',NULL)",
                draftSchedule,
                templateId,
                ShiftStore.sql(next),
                ShiftStore.sql(next.plus(Duration.ofHours(8))),
                floor);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", sameDay, giver.staffId());
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", nextDay, giver.staffId());

        var response = send(
                        "/api/v1/leave-requests",
                        giver,
                        Map.of(
                                "kind", "BY_DAY",
                                "dates", List.of(day.toString()),
                                "scopeType", "FLOOR",
                                "scopeId", floor,
                                "reason", "[Lý do nghỉ theo ngày]"))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(201);
        long id = json.readTree(response.getContentAsString())
                .get("requestIds")
                .get(0)
                .asLong();
        assertThat(leaveService.pendingLeaveDayStaff("FLOOR", floor, month)).contains(giver.staffId());

        // 008 công bố lịch tháng (trạng thái lịch, ca đổi trong lệnh công bố) rồi phát SchedulePublished.
        jdbc.update("UPDATE shift_schedule SET status='PUBLISHED' WHERE id=?", draftSchedule);
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE schedule_id=?", draftSchedule);
        transactions.executeWithoutResult(
                tx -> events.publishEvent(new com.eldercare.backend.staffing.api.events.SchedulePublished(
                        draftSchedule, List.of(giver.staffId()), time.now(), List.of())));

        assertThat(statusOf(id)).isEqualTo("PENDING_APPROVAL");
        assertThat(leaveService.pendingLeaveDayStaff("FLOOR", floor, month)).doesNotContain(giver.staffId());
        String body =
                fetch("/api/v1/leave-requests", giver).andReturn().getResponse().getContentAsString();
        var mine = new java.util.ArrayList<tools.jackson.databind.JsonNode>();
        json.readTree(body).forEach(mine::add);
        assertThat(mine.stream()
                        .filter(r -> r.get("id").asLong() == id)
                        .map(r -> r.get("kind").asString()))
                .containsExactly("BY_SHIFT");
        // Ca trong ngày nghỉ đã vào khóa (ca, nhân viên): xin nghỉ ca đó lần nữa bị chặn; ca ngày khác thì không.
        send(
                        "/api/v1/leave-requests",
                        giver,
                        Map.of("kind", "BY_SHIFT", "shiftIds", List.of(sameDay), "reason", "[Lý do]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SHIFT_IN_ANOTHER_REQUEST"));

        send("/api/v1/leave-requests/" + id + "/approve", head, null).andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("APPLIED");
        assertThat(onLeave(sameDay, giver)).isTrue();
        assertThat(onLeave(nextDay, giver)).isFalse();
    }

    @Test
    void FR_028_partialApprovalRequiresReasonForRejectedPart() throws Exception {
        long x1 = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long x2 = shift(base.plus(Duration.ofDays(1)).plus(Duration.ofHours(7)), 8, null, giver);
        long id = createByShift(giver, null, x1, x2);
        var items = List.of(Map.of("shiftId", x1, "result", "APPROVED"), Map.of("shiftId", x2, "result", "REJECTED"));
        send("/api/v1/leave-requests/" + id + "/approve", head, Map.of("items", items))
                .andExpect(status().isUnprocessableEntity());
        send("/api/v1/leave-requests/" + id + "/approve", head, Map.of("items", items, "reason", "[Thiếu người]"))
                .andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("PARTIALLY_APPLIED");
        assertThat(onLeave(x1, giver)).isTrue();
        assertThat(onLeave(x2, giver)).isFalse();
    }

    @Test
    void FR_029_d_leadMustBeChosenWhenAnotherEligibleMemberIsPresent() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, giver.staffId(), giver, recipient);
        long id = createByShift(giver, null, x);
        send("/api/v1/leave-requests/" + id + "/approve", head, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LEAD_REQUIRED"));
        send(
                        "/api/v1/leave-requests/" + id + "/approve",
                        head,
                        Map.of(
                                "items",
                                List.of(Map.of("shiftId", x, "result", "APPROVED", "newLeadId", recipient.staffId()))))
                .andExpect(status().isOk());
        Long lead = jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, x);
        assertThat(lead).isEqualTo(recipient.staffId());
    }

    @Test
    void FR_029_d_leaveIsApprovedAndShiftMarkedWithoutLeadWhenNobodyCanTakeOver() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, giver.staffId(), giver);
        long id = createByShift(giver, null, x);
        send("/api/v1/leave-requests/" + id + "/approve", head, null).andExpect(status().isOk());
        Boolean missing = jdbc.queryForObject("SELECT missing_lead FROM work_shift WHERE id=?", Boolean.class, x);
        assertThat(missing).isTrue();
    }

    @Test
    void FR_026_floorManagerFilesOnBehalf_andStaffSeesOwnReasonOnly() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createByShift(head, giver, x);
        String forGiver =
                fetch("/api/v1/leave-requests", giver).andReturn().getResponse().getContentAsString();
        String forThird =
                fetch("/api/v1/leave-requests", third).andReturn().getResponse().getContentAsString();
        assertThat(forGiver).contains("[Lý do nghỉ riêng tư]").contains("\"onBehalf\":true");
        assertThat(forThird).doesNotContain("[Lý do nghỉ riêng tư]");
        // Người không có quyền không lập thay được.
        send(
                        "/api/v1/leave-requests",
                        third,
                        Map.of(
                                "kind",
                                "BY_SHIFT",
                                "staffId",
                                giver.staffId(),
                                "shiftIds",
                                List.of(x),
                                "reason",
                                "[Lý do]"))
                .andExpect(status().isForbidden());
        send("/api/v1/leave-requests/" + id + "/cancel", head, Map.of("reason", "[Hủy]"))
                .andExpect(status().isOk());
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
    }

    @Test
    void FR_026_shiftInAnotherOpenRequestCannotBeChosen() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        createByShift(giver, null, x);
        send("/api/v1/leave-requests", giver, Map.of("kind", "BY_SHIFT", "shiftIds", List.of(x), "reason", "[Lý do]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SHIFT_IN_ANOTHER_REQUEST"));
    }

    @Test
    void FR_030_shiftThatStartsBeforeDecisionIsSplitOffAndRequestExpires() throws Exception {
        long x = shift(base.plus(Duration.ofHours(7)), 8, null, giver);
        long id = createByShift(giver, null, x);
        clock.advanceBy(Duration.ofDays(4));
        minuteJob.runNow();
        assertThat(statusOf(id)).isEqualTo("EXPIRED");
        assertThat(onLeave(x, giver)).isFalse();
    }

    @Test
    void FR_026a_dayLeaveRemovesStaffFromDraftShiftAndClearsLead() throws Exception {
        Instant future = base.plus(Duration.ofDays(45));
        YearMonth month = YearMonth.from(future.atZone(com.eldercare.backend.common.time.BusinessTime.ZONE));
        long draftSchedule = insertRow(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES (?,'FLOOR',?,'DRAFT')",
                month.toString(),
                floor);
        LocalDate day = month.atDay(10);
        Instant start = time.startOfDay(day).plus(Duration.ofHours(7));
        long draft = insertRow(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,FALSE,'DRAFT',?)",
                draftSchedule,
                templateId,
                ShiftStore.sql(start),
                ShiftStore.sql(start.plus(Duration.ofHours(8))),
                floor,
                giver.staffId());
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", draft, giver.staffId());
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", draft, recipient.staffId());

        var response = send(
                        "/api/v1/leave-requests",
                        giver,
                        Map.of(
                                "kind", "BY_DAY",
                                "dates", List.of(day.toString()),
                                "scopeType", "FLOOR",
                                "scopeId", floor,
                                "reason", "[Lý do nghỉ theo ngày]"))
                .andReturn()
                .getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(201);
        long id = json.readTree(response.getContentAsString())
                .get("requestIds")
                .get(0)
                .asLong();
        send("/api/v1/leave-requests/" + id + "/approve", head, null).andExpect(status().isOk());

        assertThat(statusOf(id)).isEqualTo("APPLIED");
        assertThat(inShift(draft, giver)).isFalse();
        assertThat(inShift(draft, recipient)).isTrue();
        Long lead = jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, draft);
        assertThat(lead).isNull();
        assertThat(leaveService.approvedLeaveDay(giver.staffId(), day)).isTrue();
        // Một nghỉ theo ngày mỗi (nhân viên, ngày), DBR nhóm 3
        send(
                        "/api/v1/leave-requests",
                        giver,
                        Map.of(
                                "kind", "BY_DAY",
                                "dates", List.of(day.toString()),
                                "scopeType", "FLOOR",
                                "scopeId", floor,
                                "reason", "[Lý do nghỉ theo ngày]"))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_026a_dayLeaveIsRefusedWhenTheMonthIsAlreadyPublished() throws Exception {
        LocalDate day =
                base.atZone(com.eldercare.backend.common.time.BusinessTime.ZONE).toLocalDate();
        send(
                        "/api/v1/leave-requests",
                        giver,
                        Map.of(
                                "kind", "BY_DAY",
                                "dates", List.of(day.toString()),
                                "scopeType", "FLOOR",
                                "scopeId", floor,
                                "reason", "[Lý do]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MONTH_ALREADY_PUBLISHED"));
    }
}
