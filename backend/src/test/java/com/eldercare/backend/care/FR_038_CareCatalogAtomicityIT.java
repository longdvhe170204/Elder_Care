package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;

/** T007: không có transaction bao test,audit lỗi phải rollback cả lệnh. */
@AutoConfigureMockMvc
@WithMockUser
@Import(FR_028a_ReasonCatalogHttpIT.Users.class)
class FR_038_CareCatalogAtomicityIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    SwitchableUser user;

    @Autowired
    BusinessTime time;

    @MockitoSpyBean
    AuditLogApi audit;

    @ParameterizedTest
    @ValueSource(
            strings = {
                "task-types:create",
                "task-types:update",
                "task-types:deactivate",
                "result-values:create",
                "result-values:update",
                "result-values:deactivate"
            })
    void FR_043_auditFailureRollsBackEveryT007Command(String scenario) throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        String[] parts = scenario.split(":");
        String path = parts[0], command = parts[1];
        String url = "/api/v1/" + path, name = "Rollback " + TestUsers.nextId();
        long id = 0;
        if (!command.equals("create")) {
            mvc.perform(post(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "before-" + TestUsers.nextId())
                            .content(FR_038_CareCatalogHttpIT.body(path, name)))
                    .andExpect(status().isCreated());
            id = jdbc.queryForObject("SELECT id FROM catalog_item WHERE name=?", Long.class, name);
        }
        AuditLogApi target = AopTestUtils.getUltimateTargetObject(audit);
        doThrow(new IllegalStateException("catalog audit unavailable"))
                .when(target)
                .write(any());
        var request = switch (command) {
            case "create" -> post(url).content(FR_038_CareCatalogHttpIT.body(path, name));
            case "update" ->
                put(url + "/" + id)
                        .header("If-Match", "\"0\"")
                        .content(FR_038_CareCatalogHttpIT.body(path, name + " sửa"));
            default -> post(url + "/" + id + "/deactivate").content("{\"reason\":\"Thử hoàn tác\"}");
        };
        assertThatThrownBy(() -> mvc.perform(request.contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "rollback-" + TestUsers.nextId())))
                .hasRootCauseMessage("catalog audit unavailable");
        if (command.equals("create"))
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog_item WHERE name=?", Integer.class, name))
                    .isZero();
        else
            assertThat(jdbc.queryForMap("SELECT name,status,version FROM catalog_item WHERE id=?", id))
                    .containsEntry("name", name)
                    .containsEntry("status", "ACTIVE")
                    .containsEntry("version", 0);
    }
}
