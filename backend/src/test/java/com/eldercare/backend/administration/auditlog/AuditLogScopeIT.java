package com.eldercare.backend.administration.auditlog;

import static com.eldercare.backend.administration.auditlog.AuditLogFixtures.MANAGER;
import static com.eldercare.backend.administration.auditlog.AuditLogFixtures.NURSE;
import static com.eldercare.backend.administration.auditlog.AuditLogFixtures.PROFILE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US3 #7, #10; FR-049, NFR-08 (000 T063, T068, T069; quickstart K19): nhân viên chỉ xem lịch sử đối tượng trong phạm vi;
 * người thân, người cao tuổi bị chặn; chỉ Quản lý viện tra cứu toàn viện theo đối tượng, người, hành động, thời gian,
 * sắp theo thời điểm giảm dần, phân trang theo khóa.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(AuditLogFixtures.class)
class AuditLogScopeIT extends MySqlIntegrationTestBase {

    private static final AuditAction NOTE_ADDED = new AuditAction("SAMPLE_NOTE_ADDED", false);
    private static final AuditAction NOTE_REMOVED = new AuditAction("SAMPLE_NOTE_REMOVED", false);

    @Autowired
    AuditLogApi auditLog;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakePermissions permissions;

    @Autowired
    TestClockControl clock;

    @Autowired
    MockMvc mvc;

    @AfterEach
    void reset() {
        user.set(MANAGER);
        clock.reset();
    }

    private void write(CurrentUser as, AuditAction action, String type, String id) {
        user.set(as);
        tx.executeWithoutResult(s -> auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(type, id)
                .after(Map.of("note", "ghi chú"))
                .build()));
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private static String history(String type, String id) {
        return "/api/v1/audit-logs/entities/" + type + "/" + id;
    }

    @Test
    void US3_7_staffInScope_seesHistory_outOfScope_isNotFound() throws Exception {
        String inScope = newId();
        String outOfScope = newId();
        write(MANAGER, NOTE_ADDED, PROFILE, inScope);
        write(MANAGER, NOTE_ADDED, PROFILE, outOfScope);
        permissions.outOfScope(PROFILE, outOfScope);

        user.set(NURSE);
        mvc.perform(get(history(PROFILE, inScope)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get(history(PROFILE, outOfScope))).andExpect(status().isNotFound());
    }

    private String read(String url) throws Exception {
        return mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
