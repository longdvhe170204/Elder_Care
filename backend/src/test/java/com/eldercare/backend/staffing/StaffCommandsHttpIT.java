package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/** T035/T048: HTTP và quyền thật; hợp đồng không thêm trường lệnh. */
@AutoConfigureMockMvc
class StaffCommandsHttpIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    JsonMapper json;

    private String manager;
    private long staff;

    @BeforeEach
    void setup() {
        var users = new TestUsers(jdbc, time);
        manager = TestUsers.bearer(
                users.session(users.staff(Role.NURSING_HOME_MANAGER).accountId()));
        staff = AssignmentFixtures.insert(
                jdbc, "INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên HTTP Phase 6]','Điều dưỡng')");
    }

    private org.springframework.test.web.servlet.ResultActions command(String path, Object body) throws Exception {
        return mvc.perform(post("/api/v1" + path)
                .header("Authorization", manager)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)));
    }

    @Test
    void FR_001a_departmentCatalogueAndChangeEndpointWorkWithoutEditingProfile() throws Exception {
        mvc.perform(get("/api/v1/departments").header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
        long dept = jdbc.queryForObject(
                "SELECT id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.leadStaffId'),'null') AS UNSIGNED) AS lead_staff_id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT') department WHERE name='Chăm sóc'",
                Long.class);
        command("/staff/" + staff + "/change-department", Map.of("departmentId", dept, "reason", "Điều chuyển bộ phận"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT COALESCE(c.m09_legacy_id,c.id) FROM staff_department d JOIN catalog_item c ON c.id=d.department_id AND c.catalog_type='M09.DEPARTMENT' WHERE d.staff_id=? AND d.ends_at IS NULL",
                        Long.class,
                        staff))
                .isEqualTo(dept);
        command("/departments/" + dept + "/assign-lead", Map.of("staffId", staff, "reason", "Chỉ định phụ trách"))
                .andExpect(status().isConflict());
    }
}
