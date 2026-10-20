package com.cambae.backend.m15.auditlog;

import static com.cambae.backend.m15.auditlog.AuditLogFixtures.MANAGER;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.PROFILE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 002 T016, BR-M15-22: dòng nhật ký do công cụ khởi tạo ghi ({@code actor_type = BOOTSTRAP}) tra cứu được, người thực
 * hiện trả về là {@code BOOTSTRAP}, không có id (contracts/api-000.yaml {@code Actor.type}).
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(AuditLogFixtures.class)
class AuditLogBootstrapActorIT extends MySqlIntegrationTestBase {

    @Autowired
    AuditLogApi auditLog;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    SwitchableUser user;

    @Autowired
    MockMvc mvc;

    @Test
    void BR_M15_22_bootstrapEntry_isReadableWithBootstrapActor() throws Exception {
        String id = UUID.randomUUID().toString();
        tx.executeWithoutResult(s -> auditLog.writeAsBootstrap(AuditLogEntry.builder()
                .action(new AuditAction("SAMPLE_BOOTSTRAPPED", false))
                .entity(PROFILE, id)
                .basis(new Basis("BR-M15-22", null, null, null))
                .build()));
        user.set(MANAGER);

        mvc.perform(get("/api/v1/audit-logs/entities/" + PROFILE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].actor.type").value("BOOTSTRAP"))
                .andExpect(jsonPath("$.items[0].actor.id").doesNotExist());
    }
}
