package com.cambae.backend.m15.auditlog;

import static com.cambae.backend.m15.auditlog.AuditLogFixtures.MANAGER;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.OFFICE;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.PROFILE;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.UNCLASSIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
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
 * US3 #8; BR-M15-11, FR-049 (000 T062; quickstart K18): giá trị trước/sau của trường người xem không được xem hiện
 * {@code {"changed": true}}, có trường bị che thì lý do bị ẩn; loại đối tượng không phân loại trường thì chỉ Quản lý
 * viện xem được.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(AuditLogFixtures.class)
class AuditLogFieldMaskingIT extends MySqlIntegrationTestBase {

    private static final AuditAction PROFILE_UPDATED = new AuditAction("SAMPLE_PROFILE_UPDATED", true);

    @Autowired
    AuditLogApi auditLog;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    SwitchableUser user;

    @Autowired
    MockMvc mvc;

    @AfterEach
    void resetUser() {
        user.set(MANAGER);
    }

    private String writeProfileChange(String type) {
        String id = UUID.randomUUID().toString();
        user.set(MANAGER);
        tx.executeWithoutResult(s -> auditLog.write(AuditLogEntry.builder()
                .action(PROFILE_UPDATED)
                .entity(type, id)
                .before(Map.of("address", "12 Lê Lợi", "allergies", "Chưa ghi nhận"))
                .after(Map.of("address", "34 Trần Phú", "allergies", "Penicillin"))
                .reason(new Reason("Cập nhật theo giấy tờ, phát hiện dị ứng penicillin"))
                .build()));
        return id;
    }

    private String historyUrl(String type, String id) {
        return "/api/v1/audit-logs/entities/" + type + "/" + id;
    }

    @Test
    void US3_8_officeStaff_seesContactValues_healthOnlyChanged_reasonHidden() throws Exception {
        String id = writeProfileChange(PROFILE);
        user.set(OFFICE);

        mvc.perform(get(historyUrl(PROFILE, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].dataBefore.address").value("12 Lê Lợi"))
                .andExpect(jsonPath("$.items[0].dataAfter.address").value("34 Trần Phú"))
                .andExpect(jsonPath("$.items[0].dataBefore.allergies.changed").value(true))
                .andExpect(jsonPath("$.items[0].dataAfter.allergies.changed").value(true))
                .andExpect(jsonPath("$.items[0].reason").value(nullValue()))
                .andExpect(jsonPath("$.items[0].reasonHidden").value(true));
    }

    @Test
    void BR_M15_11_manager_seesAllFieldsAndReason() throws Exception {
        String id = writeProfileChange(PROFILE);
        user.set(MANAGER);

        mvc.perform(get(historyUrl(PROFILE, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].dataAfter.allergies").value("Penicillin"))
                .andExpect(jsonPath("$.items[0].reason").value("Cập nhật theo giấy tờ, phát hiện dị ứng penicillin"))
                .andExpect(jsonPath("$.items[0].reasonHidden").value(false))
                .andExpect(jsonPath("$.items[0].action").value("SAMPLE_PROFILE_UPDATED"))
                .andExpect(jsonPath("$.items[0].entity.type").value(PROFILE))
                .andExpect(jsonPath("$.items[0].entity.id").value(id))
                .andExpect(jsonPath("$.items[0].actor.type").value("USER"))
                .andExpect(jsonPath("$.items[0].actor.id").value(MANAGER.accountId()))
                .andExpect(jsonPath("$.items[0].actor.role").value("AC-01"));
    }

    @Test
    void FR_049_typeWithoutClassification_onlyManagerSeesHistory() throws Exception {
        String id = writeProfileChange(UNCLASSIFIED);

        user.set(OFFICE);
        mvc.perform(get(historyUrl(UNCLASSIFIED, id))).andExpect(status().isNotFound());

        user.set(MANAGER);
        mvc.perform(get(historyUrl(UNCLASSIFIED, id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].dataAfter.allergies").value("Penicillin"));
    }

    @Test
    void FR_049_valuesAreMaskedOnServer_notSentToClient() throws Exception {
        String id = writeProfileChange(PROFILE);
        user.set(OFFICE);

        String body = mvc.perform(get(historyUrl(PROFILE, id)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).doesNotContain("Penicillin").doesNotContain("penicillin");
    }
}
