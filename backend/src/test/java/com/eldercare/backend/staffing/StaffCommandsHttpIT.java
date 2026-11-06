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
import org.springframework.mock.web.MockMultipartFile;
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
    void FR_003_employmentEndpointsEnforceReasonEffectiveTimeAndCurrentState() throws Exception {
        command(
                        "/staff/" + staff + "/terminate-employment",
                        Map.of(
                                "reason",
                                "Theo quyết định",
                                "effectiveAt",
                                time.now().plusSeconds(60)))
                .andExpect(status().isOk());
        command("/staff/" + staff + "/rehire", Map.of("reason", "Nhận lại theo quyết định"))
                .andExpect(status().isUnprocessableEntity());
        command(
                        "/staff/" + staff + "/terminate-employment",
                        Map.of(
                                "reason",
                                "Theo quyết định",
                                "effectiveAt",
                                time.now().minusSeconds(60)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("errorCode").value("INVALID_EFFECTIVE_TIME"));
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

    @Test
    void FR_006_renewRevokeCorrectAreDistinctCommands() throws Exception {
        long type = jdbc.queryForObject(
                "SELECT id FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,COALESCE(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacyCode'),'null'),c.code) AS code,NULLIF(JSON_UNQUOTE(c.attributes->'$.credentialGroup'),'null') AS credential_group,(c.attributes->'$.hasExpiry'=CAST('true' AS JSON)) AS has_expiry,c.attributes->'$.requiredForRoles' AS required_for_roles,(c.attributes->'$.hasPracticeScopes'=CAST('true' AS JSON)) AS has_practice_scopes FROM catalog_item c WHERE c.catalog_type='M09.CREDENTIAL_TYPE') credential_type WHERE code='FIRST_AID_TRAINING'",
                Long.class);
        var body = new LinkedHashMap<String, Object>();
        body.put("credentialTypeId", type);
        body.put("issueDate", time.today().minusDays(10));
        body.put("expiryDate", time.today().plusDays(10));
        command("/staff/" + staff + "/credentials", body).andExpect(status().isCreated());
        long id = jdbc.queryForObject("SELECT id FROM credential WHERE staff_id=?", Long.class, staff);
        body.put("reason", "Sửa đúng bản gốc");
        command("/credentials/" + id + "/correct", body).andExpect(status().isOk());
        body.remove("reason");
        command("/credentials/" + id + "/renew", body).andExpect(status().isCreated());
        long renewed = jdbc.queryForObject("SELECT id FROM credential WHERE replaces_id=?", Long.class, id);
        command(
                        "/credentials/" + renewed + "/revoke",
                        Map.of("effectiveFrom", time.today().plusDays(1), "reason", "Thu hồi theo quyết định"))
                .andExpect(status().isOk());
    }

    @Test
    void FR_001b_templatePreviewAndConfirmUseBinaryAndMultipart() throws Exception {
        mvc.perform(get("/api/v1/staff/import/template").header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(content().contentType(StaffImportIT.MIME));
        String phone = String.format("08%08d", TestUsers.nextId() % 100000000L);
        byte[] data = StaffImportIT.file(
                List.of(Arrays.asList("[Import HTTP]", "Điều dưỡng", phone, "25/10/2026", null, null)));
        var file = new MockMultipartFile("file", "nhan-vien.xlsx", StaffImportIT.MIME, data);
        mvc.perform(multipart("/api/v1/staff/import/preview").file(file).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("canConfirm").value(true));
        mvc.perform(multipart("/api/v1/staff/import/confirm")
                        .file(file)
                        .header("Authorization", manager)
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("createdProfileCount").value(1));
        mvc.perform(multipart("/api/v1/staff/import/confirm")
                        .file(file)
                        .header("Authorization", manager)
                        .header("Idempotency-Key", UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("errors[0].errorCode").value("DUPLICATE_STAFF_PROFILE"));
    }

    @Test
    void FR_006_practiceScopeCatalogueExposesCodeUsedByCredentialCommand() throws Exception {
        String code = "SCOPE-HTTP-" + TestUsers.nextId();
        long id = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,code,name,status,attributes) SELECT 'M09.PRACTICE_SCOPE','M09',src.code,src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'legacyCode',src.code) FROM (SELECT ? AS code,? AS name) src",
                code,
                "[Phạm vi HTTP " + code + "]");
        try {
            var response = mvc.perform(get("/api/v1/practice-scopes").header("Authorization", manager))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();
            var rows = json.readTree(response.getContentAsString());
            boolean found = false;
            for (var row : rows)
                if (row.path("id").asLong() == id) {
                    assertThat(row.path("code").asString()).isEqualTo(code);
                    found = true;
                }
            assertThat(found).isTrue();
        } finally {
            try (var c = retentionConnection();
                    var p = c.prepareStatement(
                            "DELETE FROM catalog_item c WHERE c.catalog_type='M09.PRACTICE_SCOPE' AND (COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id)=?)")) {
                p.setLong(1, id);
                p.executeUpdate();
            }
        }
    }
}
