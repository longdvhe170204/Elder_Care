package com.eldercare.backend.roombedallocation;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.sql.Timestamp;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 003 T063 (chạy lại US2, US7, US8 với bean thật thay T002, 2026-11-11): không có {@code FakeM03Config},
 * {@code FakeM15Config}; nguồn lưu trú đọc hợp đồng thật của 004 ({@code M02AllocationResidencySource}), tình trạng
 * lây nhiễm của 007 ({@code IncidentAllocationStatus}), tài sản giường của 019 ({@code BedAssets}), phân công Trưởng tầng
 * của 008/002 đều thật. US4 (giữ giường khi vắng, trở về) chạy thật ở {@code AdmissionRealBedIT}.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(SwitchableUserConfig.class)
class T063_RealSourcesIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    RealCareFixture fixture;
    RealCareFixture.Place place;
    TestUser office, director, floorManager;

    @BeforeEach
    void realData() {
        fixture = new RealCareFixture(jdbc, time);
        place = fixture.place();
        office = fixture.users().staff(Role.OFFICE_STAFF);
        director = fixture.users().staff(Role.NURSING_HOME_MANAGER);
        floorManager = fixture.staff(Role.FLOOR_MANAGER);
        fixture.floorManager(floorManager.staffId(), place.floor(), time.now().minus(Duration.ofDays(30)));
    }

    @AfterEach
    void restore() {
        user.reset();
    }

    private ResultActions allocate(long resident, long bed) throws Exception {
        user.set(office.user());
        return mvc.perform(post("/api/v1/bed-allocations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident + ",\"bedId\":" + bed + ",\"reason\":\"Bố trí giường\"}"));
    }

    /** US2: loại lưu trú theo hợp đồng thật của 004 quyết định phân bổ được hay không. */
    @Test
    void US2_residencyTypeFromRealContract_allowsLongTermAndBlocksDayCare() throws Exception {
        long longTerm = fixture.resident(
                "ADMISSION_IN_PROGRESS", "LONG_TERM_RESIDENTIAL", time.now().minus(Duration.ofDays(1)));
        long dayCare =
                fixture.resident("ADMISSION_IN_PROGRESS", "DAY_CARE", time.now().minus(Duration.ofDays(1)));
        allocate(longTerm, fixture.emptyBed(place))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FUTURE"));
        allocate(dayCare, fixture.emptyBed(place))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code").value(hasItem("ALLOCATION_RESIDENCY_TYPE")));
    }

    /** US7: phòng bị cách ly thì phân bổ vào giường của phòng bị chặn; tình trạng lây nhiễm của 007 thật. */
    @Test
    void US7_isolatedRoomBlocksAllocation_withRealInfectionStatus() throws Exception {
        long resident = fixture.resident(
                "ADMISSION_IN_PROGRESS", "LONG_TERM_RESIDENTIAL", time.now().minus(Duration.ofDays(1)));
        long bed = fixture.emptyBed(place);
        user.set(director.user());
        mvc.perform(post("/api/v1/rooms/" + place.room() + "/isolate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Phòng cần cách ly\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isolated").value(true));
        allocate(resident, bed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code").value(hasItem("ALLOCATION_ISOLATION")));
    }

    /** US8: Trưởng tầng được giao thật xác nhận giường Chờ sẵn sàng; tài sản giường 019 và lây nhiễm 007 thật. */
    @Test
    void US8_assignedFloorManagerConfirmsReadiness_withRealAssetsAndInfection() throws Exception {
        long bed = fixture.emptyBed(place);
        long departed = fixture.resident(
                "IN_RESIDENCE", "LONG_TERM_RESIDENTIAL", time.now().minus(Duration.ofDays(5)));
        long closed = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,?,'CLOSED',?,?,'BASIC','CURRENT')",
                closed,
                departed,
                bed,
                Timestamp.from(time.now().minusSeconds(7200)),
                Timestamp.from(time.now().minusSeconds(3600)));
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=?,last_closed_allocation_id=? WHERE id=?",
                Timestamp.from(time.now().minusSeconds(3600)),
                Timestamp.from(time.now().plusSeconds(10800)),
                closed,
                bed);
        user.set(floorManager.user());
        mvc.perform(get("/api/v1/beds/" + bed + "/readiness-conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
        mvc.perform(post("/api/v1/beds/" + bed + "/confirm-readiness")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Đã kiểm tra\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EMPTY"));
    }
}
