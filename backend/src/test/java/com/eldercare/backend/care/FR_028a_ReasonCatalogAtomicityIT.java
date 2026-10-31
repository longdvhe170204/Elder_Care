package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
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

/** Không có transaction bao test: lỗi audit phải hoàn tác transaction do service mở. */
@AutoConfigureMockMvc
@WithMockUser
@Import(FR_028a_ReasonCatalogHttpIT.Users.class)
class FR_028a_ReasonCatalogAtomicityIT extends MySqlIntegrationTestBase {
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
    @ValueSource(strings = {"create", "rename", "deactivate"})
    void FR_043_auditFailureRollsBackEveryReasonCatalogCommand(String command) {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        long id = TestUsers.nextId();
        String name = "Lý do rollback " + id;
        if (!command.equals("create")) {
            jdbc.update(
                    "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.NON_PERFORMANCE_REASON_TYPE','M04',?,'ACTIVE',JSON_OBJECT('fixed',FALSE))",
                    id,
                    name);
        }
        AuditLogApi target = AopTestUtils.getUltimateTargetObject(audit);
        doThrow(new IllegalStateException("audit unavailable")).when(target).write(any());
        String url = FR_028a_ReasonCatalogHttpIT.URL;
        var request = switch (command) {
            case "create" -> post(url).content("{\"name\":\"" + name + "\"}");
            case "rename" -> put(url + "/" + id).header("If-Match", "\"0\"").content("{\"name\":\"" + name + " sửa\"}");
            default -> post(url + "/" + id + "/deactivate").content("{\"reason\":\"Thử hoàn tác\"}");
        };
        assertThatThrownBy(() -> mvc.perform(request.header("Idempotency-Key", "atomic-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)))
                .hasRootCauseMessage("audit unavailable");
        if (command.equals("create")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog_item WHERE name=?", Integer.class, name))
                    .isZero();
        } else {
            assertThat(jdbc.queryForMap("SELECT name,status,version FROM catalog_item WHERE id=?", id))
                    .containsEntry("name", name)
                    .containsEntry("status", "ACTIVE")
                    .containsEntry("version", 0);
        }
    }
}
