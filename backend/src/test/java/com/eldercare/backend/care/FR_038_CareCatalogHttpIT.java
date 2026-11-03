package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.TaskTypeApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** T007: tám operation thật, quyền Q-391 và chứng chỉ áp ngay cho consumer. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import(FR_028a_ReasonCatalogHttpIT.Users.class)
class FR_038_CareCatalogHttpIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void manager() {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
    }

    static String body(String path, String name) {
        return path.equals("task-types")
                ? "{\"name\":\"" + name
                        + "\",\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"PERFORMED\",\"defaultRole\":\"CAREGIVER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[\"CERT_A\"]}"
                : "{\"name\":\"" + name
                        + "\",\"type\":\"SLEEP_QUALITY\",\"code\":\"SLEEP_TEST\",\"ruleGroup\":\"INSOMNIA\"}";
    }

    private MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "catalog-" + TestUsers.nextId())
                .content(body);
    }

    private long create(String path) throws Exception {
        String name = "Danh mục " + TestUsers.nextId();
        mvc.perform(write(post("/api/v1/" + path), body(path, name)))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""));
        return jdbc.queryForObject("SELECT id FROM catalog_item WHERE name=?", Long.class, name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"task-types", "result-values"})
    void FR_038_createUpdateDeactivateUsesSharedCatalogAndAudits(String path) throws Exception {
        long id = create(path);
        mvc.perform(get("/api/v1/" + path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].version").value(org.hamcrest.Matchers.contains(0)));
        String updated = body(path, "Đổi " + id);
        mvc.perform(write(put("/api/v1/" + path + "/" + id).header("If-Match", "\"0\""), updated))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""));
        mvc.perform(get("/api/v1/" + path))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].version").value(org.hamcrest.Matchers.contains(1)));
        mvc.perform(write(put("/api/v1/" + path + "/" + id).header("If-Match", "\"0\""), updated))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(write(post("/api/v1/" + path + "/" + id + "/deactivate"), "{\"reason\":\"Không dùng nữa\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM catalog_item WHERE id=?", String.class, id))
                .isEqualTo("INACTIVE");
        String type = path.equals("task-types") ? "M04.TASK_TYPE" : "M04.RESULT_VALUE";
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type=? AND entity_id=?",
                        Integer.class,
                        type,
                        Long.toString(id)))
                .isEqualTo(3);
    }

    @Test
    void BR_M09_01_requirementsReadChangedCertificatesImmediately() throws Exception {
        long id = create("task-types");
        var api = context.getBean(TaskTypeApi.class);
        assertThat(api.requirements(id).certificates()).containsExactly("CERT_A");
        mvc.perform(write(
                        put("/api/v1/task-types/" + id).header("If-Match", "\"0\""),
                        body("task-types", "Chứng chỉ " + id).replace("CERT_A", "CERT_B")))
                .andExpect(status().isOk());
        assertThat(api.requirements(id).role()).isEqualTo(Role.CAREGIVER);
        assertThat(api.requirements(id).certificates()).containsExactly("CERT_B");
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"FLOOR_MANAGER", "DOCTOR", "NURSING_STAFF", "CAREGIVER"})
    void FR_038_careRolesReadBothCatalogsButCannotWrite(Role role) throws Exception {
        for (String path : List.of("task-types", "result-values")) {
            manager();
            long id = create(path);
            user.set(new TestUsers(jdbc, time).staff(role).user());
            mvc.perform(get("/api/v1/" + path)).andExpect(status().isOk());
            mvc.perform(write(post("/api/v1/" + path), body(path, "Cấm tạo"))).andExpect(status().isForbidden());
            mvc.perform(write(put("/api/v1/" + path + "/" + id).header("If-Match", "\"0\""), body(path, "Cấm sửa")))
                    .andExpect(status().isForbidden());
            mvc.perform(write(post("/api/v1/" + path + "/" + id + "/deactivate"), "{\"reason\":\"Cấm ngừng\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"KITCHEN_STAFF", "OFFICE_STAFF", "ACCOUNTANT"})
    void FR_038_otherStaffCannotReadCatalogs(Role role) throws Exception {
        user.set(new TestUsers(jdbc, time).staff(role).user());
        for (String path : List.of("task-types", "result-values"))
            mvc.perform(get("/api/v1/" + path)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"task-types", "result-values"})
    void FR_038_duplicateInvalidAndMissingHeadersDoNotWrite(String path) throws Exception {
        long id = create(path);
        String name = jdbc.queryForObject("SELECT name FROM catalog_item WHERE id=?", String.class, id);
        mvc.perform(write(post("/api/v1/" + path), body(path, name))).andExpect(status().isConflict());
        mvc.perform(write(
                        post("/api/v1/" + path),
                        body(path, "Sai " + id).replace("OTHER", "UNKNOWN").replace("SLEEP_QUALITY", "UNKNOWN")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/" + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(path, "Thiếu key")))
                .andExpect(status().isBadRequest());
        mvc.perform(write(put("/api/v1/" + path + "/" + id), body(path, "Thiếu version")))
                .andExpect(status().isBadRequest());
    }
}
