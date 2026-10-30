package com.eldercare.backend.administration.auditlog;

import static com.eldercare.backend.administration.auditlog.AuditLogFixtures.MANAGER;
import static com.eldercare.backend.administration.auditlog.AuditLogFixtures.NURSE;
import static com.eldercare.backend.administration.auditlog.AuditLogFixtures.PROFILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
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
 * US3 #12; FR-046, DBR-25 (000 T064): thời điểm trong nhật ký là giờ máy chủ; bản ghi ngoại tuyến lưu cả thời điểm trên
 * thiết bị và thời điểm đồng bộ.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(AuditLogFixtures.class)
class AuditLogOfflineIT extends MySqlIntegrationTestBase {

    private static final AuditAction RECORDED = new AuditAction("SAMPLE_RECORDED", false);

    @Autowired
    AuditLogApi auditLog;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    SwitchableUser user;

    @Autowired
    TestClockControl clock;

    @Autowired
    MockMvc mvc;

    @AfterEach
    void reset() {
        user.set(MANAGER);
        clock.reset();
    }

    private String record(Instant deviceTime) {
        String id = UUID.randomUUID().toString();
        user.set(NURSE);
        tx.executeWithoutResult(s -> auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(PROFILE, id)
                .after(Map.of("note", "đã uống nước"))
                .deviceTime(deviceTime)
                .build()));
        return id;
    }

    private String firstEntry(String id) throws Exception {
        user.set(MANAGER);
        return mvc.perform(get("/api/v1/audit-logs/entities/" + PROFILE + "/" + id))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private static Instant instantAt(String json, String path) {
        return OffsetDateTime.parse(JsonPath.<String>read(json, path)).toInstant();
    }

    @Test
    void US3_12_offlineRecord_keepsDeviceTimeAndServerSyncTime() throws Exception {
        Instant synced = OffsetDateTime.parse("2026-10-19T09:30:00+07:00").toInstant();
        Instant onDevice = OffsetDateTime.parse("2026-10-19T08:10:00+07:00").toInstant();
        clock.setTo(synced);

        String json = firstEntry(record(onDevice));

        // Đồng hồ giả lập chạy tiếp sau khi đặt giờ
        assertThat(instantAt(json, "$.items[0].occurredAt")).isBetween(synced, synced.plusSeconds(5));
        assertThat(instantAt(json, "$.items[0].deviceTime")).isEqualTo(onDevice);
    }

    @Test
    void US3_12_onlineRecord_usesServerTime_noDeviceTime() throws Exception {
        Instant server = OffsetDateTime.parse("2026-10-19T09:30:00+07:00").toInstant();
        clock.setTo(server);

        String json = firstEntry(record(null));

        assertThat(instantAt(json, "$.items[0].occurredAt")).isBetween(server, server.plusSeconds(5));
        assertThat(JsonPath.<Object>read(json, "$.items[0].deviceTime")).isNull();
    }
}
