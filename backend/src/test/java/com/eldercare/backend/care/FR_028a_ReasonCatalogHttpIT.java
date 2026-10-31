package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T007a: quyền thật Q-391, dữ liệu MySQL, giữ nguyên bốn operation đã duyệt. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import(FR_028a_ReasonCatalogHttpIT.Users.class)
class FR_028a_ReasonCatalogHttpIT extends MySqlIntegrationTestBase {
    static final String URL = "/api/v1/non-performance-reason-types";

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser reasonCatalogUser() {
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

    @BeforeEach
    void manager() {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
    }

    private long row() {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.NON_PERFORMANCE_REASON_TYPE','M04',?,'ACTIVE',JSON_OBJECT('fixed',FALSE))",
                id,
                "Lý do " + id);
        return id;
    }

    @Test
    void FR_028a_createRenameDeactivateKeepsContractShapeAndAtomicAudit() throws Exception {
        String name = "Lý do " + TestUsers.nextId();
        mvc.perform(post(URL)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""))
                .andExpect(header().string("ETag", "\"0\""));
        long id = jdbc.queryForObject("SELECT id FROM catalog_item WHERE name=?", Long.class, name);
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].fixed").value(org.hamcrest.Matchers.contains(false)))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].version").isEmpty());
        mvc.perform(put(URL + "/" + id)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + " sửa\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andExpect(header().string("ETag", "\"1\""));
        mvc.perform(post(URL + "/" + id + "/deactivate")
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Không dùng nữa\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        mvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.id == " + id + ")].status").value(org.hamcrest.Matchers.contains("INACTIVE")));
        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type='M04.NON_PERFORMANCE_REASON_TYPE' AND entity_id=? ORDER BY id",
                        String.class,
                        Long.toString(id)))
                .containsExactly(
                        "NON_PERFORMANCE_REASON_CREATED",
                        "NON_PERFORMANCE_REASON_UPDATED",
                        "NON_PERFORMANCE_REASON_DEACTIVATED");
        assertThat(jdbc.queryForObject(
                        "SELECT reason FROM audit_log WHERE entity_type='M04.NON_PERFORMANCE_REASON_TYPE' AND entity_id=? AND action='NON_PERFORMANCE_REASON_DEACTIVATED'",
                        String.class,
                        Long.toString(id)))
                .isEqualTo("Không dùng nữa");
    }

    @Test
    void FR_028a_fixedReasonCannotBeRenamedOrDeactivated() throws Exception {
        long id = jdbc.queryForObject(
                "SELECT COALESCE(JSON_EXTRACT(attributes,'$.legacy.id'),id) FROM catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND JSON_EXTRACT(attributes,'$.fixed')=TRUE",
                Long.class);
        mvc.perform(put(URL + "/" + id)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Đổi nghĩa\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REASON_TYPE_FIXED"));
        mvc.perform(post(URL + "/" + id + "/deactivate")
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Thử ngừng\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REASON_TYPE_FIXED"));
        assertThat(jdbc.queryForObject(
                        "SELECT name FROM catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND COALESCE(JSON_EXTRACT(attributes,'$.legacy.id'),id)=?",
                        String.class,
                        id))
                .isEqualTo("Người cao tuổi từ chối");
    }

    @Test
    void FR_028a_staleVersionDoesNotRenameOrAudit() throws Exception {
        long id = row();
        mvc.perform(put(URL + "/" + id)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .header("If-Match", "\"99\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Không lưu\"}"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.errorCode").value("VERSION_MISMATCH"))
                .andExpect(jsonPath("$.currentValue.version").value(0));
        assertThat(jdbc.queryForObject("SELECT name FROM catalog_item WHERE id=?", String.class, id))
                .isEqualTo("Lý do " + id);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M04.NON_PERFORMANCE_REASON_TYPE' AND entity_id=?",
                        Integer.class,
                        Long.toString(id)))
                .isZero();
    }

    @Test
    void FR_028a_duplicateActiveNameIsRejected() throws Exception {
        long id = row();
        mvc.perform(post(URL)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lý do " + id + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NAME"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void FR_028a_blankNameIsRejected(String name) throws Exception {
        mvc.perform(post(URL)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_028a_retryWithSameIdempotencyKeyDoesNotCreateTwice() throws Exception {
        String name = "Lý do " + TestUsers.nextId();
        String key = "reason-" + TestUsers.nextId();
        for (int i = 0; i < 2; i++) {
            mvc.perform(post(URL)
                            .header("Idempotency-Key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"" + name + "\"}"))
                    .andExpect(status().isCreated());
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog_item WHERE name=?", Integer.class, name))
                .isEqualTo(1);
    }

    @Test
    void FR_028a_nameBeyondContractLimitIsRejected() throws Exception {
        mvc.perform(post(URL)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_028a_missingReasonDoesNotDeactivate() throws Exception {
        long id = row();
        mvc.perform(post(URL + "/" + id + "/deactivate")
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT status FROM catalog_item WHERE id=?", String.class, id))
                .isEqualTo("ACTIVE");
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"FLOOR_MANAGER", "DOCTOR", "NURSING_STAFF", "CAREGIVER"})
    void FR_028a_careRolesCanReadButCannotChangeCatalog(Role role) throws Exception {
        long id = row();
        user.set(new TestUsers(jdbc, time).staff(role).user());
        mvc.perform(get(URL)).andExpect(status().isOk());
        mvc.perform(post(URL)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cấm tạo\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put(URL + "/" + id)
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cấm sửa\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(URL + "/" + id + "/deactivate")
                        .header("Idempotency-Key", "reason-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cấm ngừng\"}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"KITCHEN_STAFF", "OFFICE_STAFF", "ACCOUNTANT"})
    void FR_028a_unpermittedRolesCannotReadCatalog(Role role) throws Exception {
        user.set(new TestUsers(jdbc, time).staff(role).user());
        mvc.perform(get(URL)).andExpect(status().isForbidden());
    }

    @Test
    void FR_028a_missingIdempotencyKeyRejectsEveryWrite() throws Exception {
        long id = row();
        String name = "Lý do thiếu khóa " + TestUsers.nextId();
        for (String command : new String[] {"create", "rename", "deactivate"}) {
            var request = switch (command) {
                case "create" ->
                    request(org.springframework.http.HttpMethod.POST, URL).content("{\"name\":\"" + name + "\"}");
                case "rename" ->
                    request(org.springframework.http.HttpMethod.PUT, URL + "/" + id)
                            .header("If-Match", "\"0\"")
                            .content("{\"name\":\"" + name + "\"}");
                default ->
                    request(org.springframework.http.HttpMethod.POST, URL + "/" + id + "/deactivate")
                            .content("{\"reason\":\"Không dùng\"}");
            };
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        }
        assertThat(jdbc.queryForMap("SELECT name,status,version FROM catalog_item WHERE id=?", id))
                .containsEntry("name", "Lý do " + id)
                .containsEntry("status", "ACTIVE")
                .containsEntry("version", 0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog_item WHERE name=?", Integer.class, name))
                .isZero();
    }

    @Test
    void FR_028a_openApiHasExactlyApprovedOperationsAndReadFields() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var api = new io.swagger.v3.parser.OpenAPIV3Parser().readContents(json).getOpenAPI();
        String base = "/api/v1/non-performance-reason-types";
        assertThat(api.getPaths().keySet().stream()
                        .filter(p -> p.startsWith(base))
                        .toList())
                .containsExactlyInAnyOrder(base, base + "/{id}", base + "/{id}/deactivate");
        assertThat(api.getPaths().get(base).readOperationsMap().keySet())
                .containsExactlyInAnyOrder(
                        io.swagger.v3.oas.models.PathItem.HttpMethod.GET,
                        io.swagger.v3.oas.models.PathItem.HttpMethod.POST);
        assertThat(api.getPaths().get(base + "/{id}").readOperationsMap().keySet())
                .containsExactly(io.swagger.v3.oas.models.PathItem.HttpMethod.PUT);
        assertThat(api.getComponents()
                        .getSchemas()
                        .get("NonPerformanceReasonType")
                        .getProperties())
                .containsOnlyKeys("id", "name", "fixed", "status");
    }
}
