package com.eldercare.backend.roombedallocation;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers.TestUser;
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
}
