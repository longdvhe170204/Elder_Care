package com.eldercare.backend.incident;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.sql.Timestamp;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nền cho IT sự cố qua HTTP: quyền, phạm vi 002 thật, phân công giả của 008; người cao tuổi ở tầng {@link #floor},
 * Trưởng tầng của tầng đó và Điều dưỡng được phân công người cao tuổi đó (T032, T033, T066).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
@Transactional
abstract class IncidentHttpTestBase extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    IncidentCatalogService catalog;

    TestUsers users;
    long floor;
    long resident;
    TestUser floorManager;
    TestUser nurse;

    @BeforeEach
    void prepareIncidentUsers() {
        users = new TestUsers(jdbc, time);
        floor = TestUsers.nextId();
        resident = TestUsers.nextId();
        floorManager = users.staff(Role.FLOOR_MANAGER);
        assign(floorManager, AssignmentKind.FLOOR_MANAGER, floor, null);
        nurse = users.staff(Role.NURSING_STAFF);
        assign(nurse, AssignmentKind.SHIFT_MEMBER, null, resident);
        // T005 chưa nạp các loại này (chờ chủ dự án chốt mức mặc định); tạo trong transaction của test rồi rollback.
        catalogItem(
                "M05.INCIDENT_TYPE",
                "FACILITY",
                "{\"defaultLevel\":\"LOW\",\"nonHealth\":true,\"allowsNoResident\":true}");
        catalogItem(
                "M05.INCIDENT_TYPE",
                "SUSPECTED_INFECTION",
                "{\"defaultLevel\":\"MEDIUM\",\"nonHealth\":false,\"allowsNoResident\":false}");
        catalogItem("M05.PROGRESSION_TYPE", "TEST_EVALUATION", "{\"group\":\"EVALUATION\",\"resuscitation\":false}");
        catalogItem("M05.PROGRESSION_TYPE", "TEST_ACTION", "{\"group\":\"ACTION\",\"resuscitation\":false}");
    }

    private void catalogItem(String catalogType, String code, String attributes) {
        jdbc.update("""
                INSERT INTO catalog_item (catalog_type, module, code, name, sort_order, status, attributes)
                SELECT ?, 'M05', ?, CONCAT('Kiểm thử ', ?), 99, 'ACTIVE', CAST(? AS JSON) FROM DUAL
                WHERE NOT EXISTS (SELECT 1 FROM catalog_item WHERE catalog_type = ? AND code = ?)
                """, catalogType, code, code, attributes, catalogType, code);
    }

    ResultActions post(TestUser as, String path, Object id, String json) throws Exception {
        return mvc.perform(as(as, MockMvcRequestBuilders.post(path, id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    ResultActions postNew(TestUser as, String path, String json) throws Exception {
        return mvc.perform(as(as, MockMvcRequestBuilders.post(path))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    MockHttpServletRequestBuilder as(TestUser user, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", TestUsers.bearer(users.session(user.accountId())));
    }

    void assign(TestUser user, AssignmentKind kind, Long floorId, Long residentId) {
        assignments.assign(
                user.staffId(),
                new AssignmentView(
                        kind,
                        floorId,
                        null,
                        residentId,
                        TestUsers.nextId(),
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(7))));
    }

    long typeId(String code) {
        return catalog.incidentTypes().stream()
                .filter(type -> type.code().equals(code) && type.active())
                .findFirst()
                .orElseThrow()
                .id();
    }

    long progressionType(ProgressionGroup group) {
        return catalog.progressionTypes().stream()
                .filter(type -> type.group() == group && !type.resuscitation())
                .findFirst()
                .orElseThrow()
                .id();
    }

    String incidentStatus(long incident) {
        return jdbc.queryForObject("SELECT status FROM incident WHERE id = ?", String.class, incident);
    }

    /** Sự cố của {@link #resident} ở {@link #floor}; trạng thái khác Mới đặt bằng lệnh UPDATE ngay sau khi chèn. */
    long incident(String typeCode, String level, String status, Long assignee, long recordedBy) {
        long id = TestUsers.nextId();
        long type = typeId(typeCode);
        var at = Timestamp.from(time.now());
        jdbc.update(
                """
                INSERT INTO incident (id, resident_id, incident_type_id, current_type_id, type_code, discovered_at,
                  floor_area_id, description, initial_level, level, was_emergency, recorded_by, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'Mô tả sự cố', ?, ?, ?, ?, ?, ?)
                """,
                id,
                resident,
                type,
                type,
                typeCode,
                at,
                floor,
                level,
                level,
                "CRITICAL".equals(level),
                recordedBy,
                at,
                at);
        if (!"NEW".equals(status)) {
            jdbc.update("UPDATE incident SET status = ?, assignee_id = ? WHERE id = ?", status, assignee, id);
        }
        return id;
    }
}
