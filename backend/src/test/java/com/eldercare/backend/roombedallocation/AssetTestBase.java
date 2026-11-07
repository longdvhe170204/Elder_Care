package com.eldercare.backend.roombedallocation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 Phase 7 (US4): nền test tài sản. Bộ phận Y tế (Phụ trách là Bác sĩ), Chăm sóc; tầng 2 có phòng 203, Trưởng tầng
 * của tầng 2 và của tầng 1 (phân công FLOOR_MANAGER); loại hệ thống BED, VEHICLE của V019_05; một loại thiết bị.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import({AssetTestBase.Users.class, FakeM15Config.class})
abstract class AssetTestBase extends MySqlIntegrationTestBase {
    static final String URL = "/api/v1/assets";

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser assetUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeDuties duties;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    ApplicationEvents events;

    long medical, care, floor, otherFloor, room, equipmentType, bedType;
    TestUsers.TestUser medicalLead, careLead, floorManager, otherFloorManager, manager;

    @BeforeEach
    void fixture() {
        var users = new TestUsers(jdbc, time);
        medical = TestUsers.nextId();
        care = TestUsers.nextId();
        medicalLead = users.staff(Role.DOCTOR);
        careLead = users.staff(Role.FLOOR_MANAGER);
        floorManager = users.staff(Role.FLOOR_MANAGER);
        otherFloorManager = users.staff(Role.FLOOR_MANAGER);
        manager = users.staff(Role.NURSING_HOME_MANAGER);
        duties.departmentLead(medical, medicalLead.staffId()).departmentLead(care, careLead.staffId());

        long area = TestUsers.nextId();
        floor = TestUsers.nextId();
        otherFloor = TestUsers.nextId();
        long roomType = TestUsers.nextId();
        room = TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES(?,?,?)", area, "K" + area, "Khu " + area);
        jdbc.update("INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)", floor, area, "T" + floor, "Tầng 2");
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)",
                otherFloor,
                area,
                "T" + otherFloor,
                "Tầng 1");
        jdbc.update("INSERT INTO room_type(id,name,standard_beds) VALUES(?,?,2)", roomType, "Loại " + roomType);
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                room,
                floor,
                roomType,
                "P" + room,
                "Phòng 203");
        assign(floorManager, floor);
        assign(otherFloorManager, otherFloor);
        bedType = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M03.ASSET_TYPE' AND code='BED'", Long.class);
        as(manager);
        equipmentType = createdType("Thiết bị y tế " + TestUsers.nextId());
        as(medicalLead);
    }

    void assign(TestUsers.TestUser who, long floorAreaId) {
        assignments.assign(
                who.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floorAreaId,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
    }

    void as(TestUsers.TestUser who) {
        user.set(who.user());
    }

    long createdType(String name) {
        try {
            mvc.perform(post("/api/v1/asset-types")
                            .header("Idempotency-Key", "at-" + TestUsers.nextId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name + "\"}"))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M03.ASSET_TYPE' AND name=?", Long.class, name);
    }

    /** Yêu cầu mua sắm Đã duyệt của M11 (019 Phase 3), chèn thẳng để test M03 không phụ thuộc lệnh của M11. */
    long approvedRequest(long department, String type) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO procurement_request(id,department_id,created_by_staff_id,type,`lines`,estimated_total,reason,"
                        + "linked_sources,status,approved_amount,approved_at,created_at)"
                        + " VALUES(?,?,?,?,'[]',2000000,'Mua',JSON_ARRAY(),'APPROVED',2000000,?,?)",
                id,
                department,
                medicalLead.staffId(),
                type,
                LocalDateTime.now(),
                LocalDateTime.now());
        return id;
    }

    ResultActions create(String body) throws Exception {
        return mvc.perform(post(URL)
                .header("Idempotency-Key", "a-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    long created(String body) throws Exception {
        String location = create(body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    String purchased(long type, Long request, Long cost) {
        return "{\"code\":\"TS-" + TestUsers.nextId() + "\",\"typeId\":" + type + ",\"name\":\"Máy tạo oxy\","
                + "\"departmentId\":" + medical + ",\"source\":\"PURCHASED\""
                + (request == null ? "" : ",\"procurementRequestId\":" + request)
                + (cost == null ? "" : ",\"acquisitionCost\":" + cost)
                + ",\"maintenanceIntervalDays\":90}";
    }

    String donated(String description) {
        return "{\"code\":\"TS-" + TestUsers.nextId() + "\",\"typeId\":" + equipmentType + ",\"name\":\"Xe lăn\","
                + "\"departmentId\":" + medical + ",\"source\":\"DONATED\""
                + (description == null ? "" : ",\"sourceDescription\":\"" + description + "\"") + "}";
    }

    ResultActions command(long id, String command, String body) throws Exception {
        return mvc.perform(post(URL + "/" + id + "/" + command)
                .header("Idempotency-Key", "ac-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    String stateOf(long id) {
        return jdbc.queryForObject("SELECT status FROM asset WHERE id=?", String.class, id);
    }
}
