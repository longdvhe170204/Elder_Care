package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.admission.FakeContractSchedules;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AssignmentResolutionHttpIT extends AssignmentHttpTestBase {
    @Autowired
    FakeContractSchedules attendance;

    @AfterEach
    void clearAttendance() {
        attendance.clear();
    }

    private MockHttpServletRequestBuilder resolution(Instant at) {
        return get("/api/v1/shifts/" + shift + "/assignment-resolution")
                .header("Authorization", bearer)
                .param("residentId", Long.toString(resident))
                .param("at", at.toString())
                .param("role", "CAREGIVER");
    }

    private MockHttpServletRequestBuilder missing() {
        return get("/api/v1/shifts/" + shift + "/unassigned-residents").header("Authorization", bearer);
    }

    @Test
    void FR_028_resolvesAtStartAndKeepsNurseSelectionSeparate() throws Exception {
        assign(other.staffId(), "RESIDENT", resident, "NURSING_STAFF", "PRIMARY", true, null);
        publish();
        mvc.perform(resolution(from))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(worker.staffId()))
                .andExpect(jsonPath("$.assignedNurseStaffIds[0]").value(other.staffId()))
                .andExpect(jsonPath("$.taskTypeId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void FR_028_moreSpecificAndTaskSpecificAssignmentsWin() throws Exception {
        assign(other.staffId(), "ROOM", room, "CAREGIVER", "PRIMARY", false, 123L);
        publish();
        mvc.perform(resolution(from))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(worker.staffId()));
        mvc.perform(resolution(from).param("taskTypeId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(other.staffId()));
    }

    @Test
    void FR_028_endIsExclusiveAndAtAndRoleAreRequired() throws Exception {
        publish();
        mvc.perform(resolution(to)).andExpect(status().isBadRequest());
        mvc.perform(resolution(from.minusSeconds(1))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/shifts/" + shift + "/assignment-resolution").header("Authorization", bearer))
                .andExpect(status().isBadRequest());
        mvc.perform(resolution(from).param("role", "UNKNOWN")).andExpect(status().isBadRequest());
    }

    @Test
    void FR_025_neitherReadPretendsDraftIsUnassigned() throws Exception {
        mvc.perform(resolution(from))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        mvc.perform(missing()).andExpect(status().isConflict());
    }

    @Test
    void FR_028_ambiguousSourceFailsClosed() throws Exception {
        assign(other.staffId(), "FLOOR", floor, "CAREGIVER", "PRIMARY", false, null);
        publish();
        mvc.perform(resolution(from))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ASSIGNMENT_SOURCE_UNAVAILABLE"));
    }

    @Test
    void FR_028_absentMemberDoesNotCountAndEndedAssignmentUsesHistory() throws Exception {
        publish();
        jdbc.update(
                "UPDATE shift_staff SET absent_at=? WHERE shift_id=? AND staff_id=?",
                com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore.sql(from.plusSeconds(60)),
                shift,
                worker.staffId());
        mvc.perform(resolution(from))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(worker.staffId()));
        mvc.perform(resolution(from.plusSeconds(60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void FR_029_reportsOnlyMissingKindsAtShiftStartWithoutWriting() throws Exception {
        publish();
        var before = jdbc.queryForMap("SELECT version,status FROM work_shift WHERE id=?", shift);
        mvc.perform(missing())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluatedAt").value(from.toString()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].residentId").value(resident))
                .andExpect(jsonPath("$.items[0].missingAssignedNurse").value(true))
                .andExpect(jsonPath("$.items[0].missingPrimaryCaregiver").value(false));
        assertThat(jdbc.queryForMap("SELECT version,status FROM work_shift WHERE id=?", shift))
                .isEqualTo(before);
    }

    @Test
    void FR_029_completeCoverageReturnsEmptyItems() throws Exception {
        assign(other.staffId(), "RESIDENT", resident, "NURSING_STAFF", "PRIMARY", true, null);
        publish();
        mvc.perform(missing())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void FR_029_historyBeforeFinalStatusIsStillIncluded() throws Exception {
        residents.changeStatus(resident, ResidentStatus.RESIDENCY_ENDED, to.plusSeconds(60));
        publish();
        mvc.perform(missing())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].residentId").value(resident));
    }

    @Test
    void FR_029_finalBeforeShiftIsExcluded() throws Exception {
        residents.changeStatus(resident, ResidentStatus.DECEASED, from);
        publish();
        mvc.perform(missing())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void FR_029_daycareAtStartUsesApprovedAttendanceAndRestArea() throws Exception {
        long daycare = TestUsers.nextId();
        residents.resident(daycare, "[NCT bán trú kiểm thử]", null, ResidentStatus.IN_RESIDENCE);
        locations.daycareRestLocation(new com.eldercare.backend.roombedallocation.api.dto.LocationRef("ROOM", room));
        attendance.schedule(
                daycare,
                new AttendanceScheduleView(LocalDate.of(2026, 11, 7), LocalTime.of(21, 0), LocalTime.of(23, 0)));
        publish();
        mvc.perform(missing())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void FR_029_attendanceSourceFailureIsNotEmptySuccess() throws Exception {
        attendance.available(false);
        publish();
        mvc.perform(missing())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ASSIGNMENT_SOURCE_UNAVAILABLE"));
    }

    @Test
    void FR_054_namedMemberDoesNotGetWholeFloorList() throws Exception {
        publish();
        bearer = token(worker);
        mvc.perform(missing()).andExpect(status().isNotFound());
    }

    @Test
    void FR_033_unrelatedResidentCannotBeReadEvenByNamedMember() throws Exception {
        long outside = TestUsers.nextId(), outsideRoom = TestUsers.nextId(), outsideFloor = TestUsers.nextId();
        locations
                .floor(outsideFloor, null)
                .room(outsideRoom, outsideFloor)
                .stay(outside, outsideRoom, from.minusSeconds(1), null);
        residents.resident(outside, "[NCT ngoài phạm vi]", null, ResidentStatus.IN_RESIDENCE);
        publish();
        resident = outside;
        bearer = token(worker);
        mvc.perform(resolution(from)).andExpect(status().isNotFound());
    }

    @Test
    void FR_028_roomTransferUsesThePositionAtTheRequestedTime() throws Exception {
        long nextRoom = TestUsers.nextId();
        locations
                .room(nextRoom, floor)
                .closeStays(resident, from.plusSeconds(60))
                .stay(resident, nextRoom, from.plusSeconds(60), null);
        assign(other.staffId(), "ROOM", nextRoom, "CAREGIVER", "PRIMARY", false, null);
        publish();
        mvc.perform(resolution(from))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(worker.staffId()));
        mvc.perform(resolution(from.plusSeconds(60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(other.staffId()));
    }

    @Test
    void FR_025_cancelledShiftDoesNotReturnAnEffectiveResult() throws Exception {
        publish();
        jdbc.update("UPDATE work_shift SET status='CANCELLED' WHERE id=?", shift);
        mvc.perform(resolution(from)).andExpect(status().isConflict());
        mvc.perform(missing()).andExpect(status().isConflict());
    }
}
