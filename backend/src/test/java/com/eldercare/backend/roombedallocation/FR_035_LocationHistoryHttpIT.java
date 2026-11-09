package com.eldercare.backend.roombedallocation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T048: giữ dạng phản hồi của contract, không trả DTO nội bộ thay schema web. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class FR_035_LocationHistoryHttpIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.administration.account.api.PermissionApi permissions;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.resident.api.ResidentStatusApi statuses;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeDuties duties;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeAssignments assignments;

    long resident, id;

    @BeforeEach
    void historyHttpFixture() {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source,opened_by_id) VALUES(?,?,?,'CLOSED','2026-01-01 00:00:00','2026-01-02 00:00:00','BASIC','CURRENT',?)",
                id,
                resident,
                bed,
                user.accountId());
    }

    @Test
    void FR_035_residentAtAndWindowReturnContractShape() throws Exception {
        mvc.perform(get("/api/v1/location-history/residents/" + resident).param("at", "2026-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allocationId").value(id))
                .andExpect(jsonPath("$[0].location.bedId").value(bed))
                .andExpect(jsonPath("$[0].corrected").value(false));
        mvc.perform(get("/api/v1/location-history/residents/" + resident).param("at", "2026-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void FR_036_bedsAndRoommatesUseRealM01HistoryAndClipToOverlap() throws Exception {
        long otherBed = TestUsers.nextId(), other = TestUsers.nextId();
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G2')", otherBed, room);
        for (long person : java.util.List.of(resident, other))
            jdbc.update(
                    "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,?,'1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                    person,
                    person == resident ? "[Họ tên NCT 1]" : "[Họ tên NCT 2]",
                    Timestamp.from(time.now()),
                    Timestamp.from(time.now()),
                    user.accountId());
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,'CLOSED','2026-01-01 01:00:00','2026-01-01 02:00:00','BASIC','CURRENT')",
                other,
                otherBed);
        statusHistory(
                resident, "IN_RESIDENCE", "TEMPORARILY_ABSENT", "GRANT_TEMPORARY_ABSENCE", "2026-01-01T00:30:00Z");
        statusHistory(resident, "TEMPORARILY_ABSENT", "IN_RESIDENCE", "RECORD_RETURN", "2026-01-01T01:15:00Z");

        mvc.perform(get("/api/v1/location-history/beds")
                        .param("roomId", Long.toString(room))
                        .param("date", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.location.bedId==" + otherBed + ")].residents[0].residentId")
                        .value(org.hamcrest.Matchers.contains((int) other)));
        // Không stub statusPeriods: HTTP đi qua bản đọc lịch sử M01 thật, khoảng vắng cắt theo khoảng cùng phòng.
        mvc.perform(get("/api/v1/location-history/roommates")
                        .param("residentId", Long.toString(resident))
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-01-03T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].residentId").value(other))
                .andExpect(jsonPath("$[0].overlaps[0].from").value("2026-01-01T01:00:00Z"))
                .andExpect(jsonPath("$[0].overlaps[0].to").value("2026-01-01T02:00:00Z"))
                .andExpect(jsonPath("$[0].absences.length()").value(1))
                .andExpect(jsonPath("$[0].absences[0].from").value("2026-01-01T01:00:00Z"))
                .andExpect(jsonPath("$[0].absences[0].to").value("2026-01-01T01:15:00Z"));
    }

    @Test
    void FR_026_allocationAndReadinessCorrectionsOverHttp() throws Exception {
        mvc.perform(
                        post("/api/v1/bed-allocations/" + id + "/corrections")
                                .contentType("application/json")
                                .content(
                                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"endAt\":\"2026-01-02T01:00:00Z\"},\"reason\":\"Sửa mốc\"}"))
                .andExpect(status().isCreated());
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
        duties.shiftLead(floor, user.staffId().orElseThrow());
        assignments.assign(
                user.staffId().orElseThrow(),
                new com.eldercare.backend.administration.account.api.spi.AssignmentView(
                        com.eldercare.backend.administration.account.api.spi.AssignmentKind.SHIFT_LEAD,
                        floor,
                        null,
                        null,
                        TestUsers.nextId(),
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        long history = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_state_history(id,bed_id,from_state,to_state,event,occurred_at,actor_id,basis_type,basis_id,disinfection_required,disinfected,ppe_used,note,floor_area_id) VALUES(?,?,'AWAITING_READY','EMPTY','CONFIRM_READY',?,?,'M03.BED',?,FALSE,FALSE,FALSE,'Gốc',?)",
                history,
                bed,
                Timestamp.from(time.now()),
                user.accountId(),
                bed,
                floor);
        mvc.perform(
                        post("/api/v1/beds/" + bed + "/state-history/" + history + "/corrections")
                                .contentType("application/json")
                                .content(
                                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"note\":\"Ghi đúng\"},\"reason\":\"Sửa ghi chú\"}"))
                .andExpect(status().isCreated());
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        mvc.perform(get("/api/v1/beds/" + bed + "/state-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + history + ")].note")
                        .value(org.hamcrest.Matchers.contains("Ghi đúng")))
                .andExpect(jsonPath("$[?(@.id == " + history + ")].corrected")
                        .value(org.hamcrest.Matchers.contains(true)));
        org.assertj.core.api.Assertions.assertThat(
                        jdbc.queryForObject("SELECT end_at FROM v_current_allocation WHERE id=?", Timestamp.class, id)
                                .toInstant())
                .isEqualTo(java.time.Instant.parse("2026-01-02T01:00:00Z"));
    }

    private void statusHistory(long person, String from, String to, String command, String at) {
        // Lịch sử trạng thái của 001 ở status_history từ 001 T084 (data-model 001 mục 0.1)
        com.eldercare.backend.support.ResidentHistoryRows.insertStatus(
                jdbc,
                TestUsers.nextId(),
                person,
                from,
                to,
                command,
                Timestamp.from(java.time.Instant.parse(at)),
                user.accountId(),
                false,
                "Dữ liệu kiểm thử FR-036");
    }
}
