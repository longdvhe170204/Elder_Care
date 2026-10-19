package com.cambae.backend.m15.auditlog;

import static com.cambae.backend.m15.auditlog.AuditLogFixtures.FAMILY;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.MANAGER;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.NURSE;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.OFFICE;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.PROFILE;
import static com.cambae.backend.m15.auditlog.AuditLogFixtures.RESIDENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.support.FakePermissions;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    @Test
    void FR_049_familyAndResident_areBlockedFromAuditLog() throws Exception {
        String id = newId();
        write(MANAGER, NOTE_ADDED, PROFILE, id);

        for (CurrentUser blocked : List.of(FAMILY, RESIDENT)) {
            user.set(blocked);
            mvc.perform(get(history(PROFILE, id))).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/audit-logs")).andExpect(status().isForbidden());
        }
    }

    @Test
    void FR_049_facilitySearch_isManagerOnly() throws Exception {
        for (CurrentUser staff : List.of(NURSE, OFFICE)) {
            user.set(staff);
            mvc.perform(get("/api/v1/audit-logs"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("PERMISSION_DENIED"));
        }
        user.set(MANAGER);
        mvc.perform(get("/api/v1/audit-logs")).andExpect(status().isOk());
    }

    @Test
    void US3_10_manager_filtersByEntityActorActionAndTime_newestFirst() throws Exception {
        String id = newId();
        clock.setTo(Instant.parse("2026-03-01T01:00:00Z"));
        write(NURSE, NOTE_ADDED, PROFILE, id);
        clock.setTo(Instant.parse("2026-03-02T01:00:00Z"));
        write(OFFICE, NOTE_ADDED, PROFILE, id);
        clock.setTo(Instant.parse("2026-03-03T01:00:00Z"));
        write(NURSE, NOTE_REMOVED, PROFILE, id);
        clock.reset();
        user.set(MANAGER);

        mvc.perform(get("/api/v1/audit-logs").param("entityType", PROFILE).param("entityId", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].action").value("SAMPLE_NOTE_REMOVED"))
                .andExpect(jsonPath("$.items[2].actor.id").value(NURSE.accountId()));

        mvc.perform(get("/api/v1/audit-logs")
                        .param("entityId", id)
                        .param("entityType", PROFILE)
                        .param("actorId", String.valueOf(NURSE.accountId())))
                .andExpect(jsonPath("$.items.length()").value(2));

        mvc.perform(get("/api/v1/audit-logs")
                        .param("entityType", PROFILE)
                        .param("entityId", id)
                        .param("action", "SAMPLE_NOTE_REMOVED", "SAMPLE_UNUSED"))
                .andExpect(jsonPath("$.items.length()").value(1));

        mvc.perform(get("/api/v1/audit-logs")
                        .param("entityType", PROFILE)
                        .param("entityId", id)
                        .param("from", "2026-03-02T00:00:00Z")
                        .param("to", "2026-03-02T23:59:59Z"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].actor.id").value(OFFICE.accountId()));
    }

    @Test
    void US3_10_keysetPagination_returnsEveryEntryOnce() throws Exception {
        String id = newId();
        for (int i = 0; i < 55; i++) {
            write(NURSE, NOTE_ADDED, PROFILE, id);
        }
        user.set(MANAGER);
        String facility = "/api/v1/audit-logs?entityType=" + PROFILE + "&entityId=" + id;

        String first = read(facility);
        String cursor = JsonPath.read(first, "$.nextCursor");
        String second = read(facility + "&cursor=" + cursor);
        List<String> ids = new ArrayList<>(JsonPath.<List<String>>read(first, "$.items[*].id"));
        ids.addAll(JsonPath.<List<String>>read(second, "$.items[*].id"));

        assertThat(cursor).isNotNull();
        assertThat(ids).hasSize(55).doesNotHaveDuplicates();
        assertThat(JsonPath.<Object>read(second, "$.nextCursor")).isNull();
        // Lịch sử một đối tượng dùng cùng cách phân trang
        String historySecond = read(history(PROFILE, id) + "?cursor=" + cursor);
        assertThat(JsonPath.<List<String>>read(historySecond, "$.items[*].id"))
                .isEqualTo(JsonPath.<List<String>>read(second, "$.items[*].id"));
    }

    private String read(String url) throws Exception {
        return mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
