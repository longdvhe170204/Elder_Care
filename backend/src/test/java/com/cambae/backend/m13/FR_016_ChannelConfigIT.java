package com.cambae.backend.m13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 009 US3 (T027, T028): Quản lý viện bật/tắt kênh, gắn kênh khác vào mức, bắt buộc lý do, ghi nhật ký (FR-016, FR-040);
 * chống ghi đè theo ETag của danh sách kênh (412); vai trò khác không xem, không cấu hình (FR-039).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class FR_016_ChannelConfigIT extends M13IntegrationTestBase {

    private static final String URL = "/api/v1/notification-channels";

    @Autowired
    BusinessTime time;

    @Autowired
    MockMvc mvc;

    TestUsers users;
    String manager;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        TestUser m = users.staff(Role.NURSING_HOME_MANAGER);
        manager = TestUsers.bearer(users.session(m.accountId()));
    }

    @AfterEach
    void restore() {
        jdbc.update("UPDATE notification_channel SET enabled = FALSE, levels = 'URGENT' WHERE code = 'SMS'");
    }

    private String etag() throws Exception {
        return mvc.perform(get(URL).header("Authorization", manager))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    private ResultActions change(String etag, String body) throws Exception {
        return mvc.perform(put(URL + "/SMS")
                .header("Authorization", manager)
                .header("If-Match", etag)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void FR_016_managerEnablesSms_withReason_logged() throws Exception {
        mvc.perform(get(URL).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'SMS')].enabled").value(false));
        String etag = etag();

        change(etag, "{\"code\":\"SMS\",\"enabled\":true,\"reason\":\"Đã có nhà cung cấp\"}")
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT enabled FROM notification_channel WHERE code = 'SMS'", Boolean.class))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT reason FROM audit_log WHERE action = 'NOTIFICATION_CHANNEL_CHANGED' AND entity_id = 'SMS'"
                                + " ORDER BY id DESC LIMIT 1",
                        String.class))
                .isEqualTo("Đã có nhà cung cấp");
        change(etag, "{\"code\":\"SMS\",\"enabled\":false,\"reason\":\"Ghi đè\"}")
                .andExpect(status().isPreconditionFailed());
    }

    @Test
    void FR_040_reasonRequired_FR_039_otherRolesForbidden() throws Exception {
        change(etag(), "{\"code\":\"SMS\",\"enabled\":true}").andExpect(status().isUnprocessableContent());

        TestUser office = users.staff(Role.OFFICE_STAFF);
        mvc.perform(get(URL).header("Authorization", TestUsers.bearer(users.session(office.accountId()))))
                .andExpect(status().isForbidden());
    }
}
