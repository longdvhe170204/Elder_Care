package com.eldercare.backend.administration.parameter;

import static com.eldercare.backend.administration.parameter.ParameterFixtures.MANAGER;
import static com.eldercare.backend.administration.parameter.ParameterFixtures.MANAGER_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * US4 #1–3, #5, #7, #9; DBR-24, FR-014, FR-015, FR-020, FR-021 (000 T075; quickstart K1–K3): đổi giá trị cập nhật giá
 * trị hiện hành bằng dòng lịch sử mới nhất, ghi nhật ký trước/sau; If-Match cũ trả 412 kèm giá trị hiện hành; khôi phục
 * mặc định là một lần đổi; giá trị sai giữ nguyên giá trị hiện hành.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(ParameterFixtures.class)
class ParameterChangeIT extends MySqlIntegrationTestBase {

    private static final String ALERT = "CFG-M05-01";
    private static final String MINUTES_20 = "{\"value\":20,\"unit\":\"MINUTES\"}";

    @Autowired
    MockMvc mvc;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    private ParameterClient api;

    @BeforeEach
    void setUp() {
        api = new ParameterClient(mvc);
        user.set(MANAGER);
    }

    @AfterEach
    void restore() throws Exception {
        user.set(MANAGER);
        api.restoreDefault(ALERT, api.etag(ALERT), "khôi phục sau test");
    }

    private int historyRows(String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM parameter_history WHERE code = ?", Integer.class, code);
    }

    @Test
    void US4_2_change_updatesCurrentValue_addsHistory_andAuditBeforeAfter() throws Exception {
        String etag = api.etag(ALERT);
        int before = historyRows(ALERT);
        int logs = jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = 'PARAMETER_CHANGED' AND entity_id = ?",
                Integer.class,
                ALERT);

        api.change(ALERT, etag, MINUTES_20, "Tăng thời hạn tiếp nhận")
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.code").value(ALERT))
                .andExpect(jsonPath("$.currentValue.value").value(20))
                .andExpect(jsonPath("$.defaultValue.value").value(15));

        assertThat(historyRows(ALERT)).isEqualTo(before + 1);
        // DBR-24: giá trị hiện hành bằng giá trị của dòng lịch sử mới nhất
        assertThat(jdbc.queryForObject(
                        "SELECT p.current_value = h.value FROM parameter p JOIN parameter_history h ON h.code = p.code"
                                + " WHERE p.code = ? ORDER BY h.effective_from DESC LIMIT 1",
                        Boolean.class,
                        ALERT))
                .isTrue();
        Map<String, Object> log = jdbc.queryForMap(
                "SELECT actor_id, reason, JSON_EXTRACT(data_before, '$.value.value') AS before_value,"
                        + " JSON_EXTRACT(data_after, '$.value.value') AS after_value FROM audit_log"
                        + " WHERE action = 'PARAMETER_CHANGED' AND entity_type = 'M15.PARAMETER' AND entity_id = ?"
                        + " ORDER BY id DESC LIMIT 1",
                ALERT);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'PARAMETER_CHANGED' AND entity_id = ?",
                        Integer.class,
                        ALERT))
                .isEqualTo(logs + 1);
        assertThat(log)
                .containsEntry("actor_id", MANAGER.accountId())
                .containsEntry("reason", "Tăng thời hạn tiếp nhận");
        assertThat(String.valueOf(log.get("before_value"))).isEqualTo("15");
        assertThat(String.valueOf(log.get("after_value"))).isEqualTo("20");
    }

    @Test
    void US4_9_staleIfMatch_isRejectedWithCurrentValue_thenSavesAfterReload() throws Exception {
        String seen = api.etag(ALERT);
        user.set(MANAGER_2);
        api.change(ALERT, seen, MINUTES_20, "Người thứ nhất lưu").andExpect(status().isOk());

        user.set(MANAGER);
        api.change(ALERT, seen, "{\"value\":30,\"unit\":\"MINUTES\"}", "Người thứ hai lưu")
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.errorCode").value("VERSION_MISMATCH"))
                .andExpect(jsonPath("$.currentValue.currentValue.value").value(20));

        api.change(ALERT, api.etag(ALERT), "{\"value\":30,\"unit\":\"MINUTES\"}", "Lưu lại sau khi thấy 20")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue.value").value(30));
    }

    @Test
    void US4_7_restoreDefault_isAnOrdinaryChange_markedAsRestore() throws Exception {
        api.change(ALERT, api.etag(ALERT), MINUTES_20, "Thử giá trị mới").andExpect(status().isOk());

        api.restoreDefault(ALERT, api.etag(ALERT), "Trở về mặc định")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue.value").value(15));

        Map<String, Object> last = jdbc.queryForMap(
                "SELECT is_default_restore, reason, JSON_EXTRACT(value, '$.value') AS v FROM parameter_history"
                        + " WHERE code = ? ORDER BY effective_from DESC LIMIT 1",
                ALERT);
        assertThat(last).containsEntry("is_default_restore", true).containsEntry("reason", "Trở về mặc định");
        assertThat(String.valueOf(last.get("v"))).isEqualTo("15");
    }

    @Test
    void US4_5_invalidValue_isRejected_currentValueKept() throws Exception {
        int before = historyRows(ALERT);

        api.change(ALERT, api.etag(ALERT), "{\"value\":0,\"unit\":\"MINUTES\"}", "Sai khoảng")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));

        assertThat(historyRows(ALERT)).isEqualTo(before);
        mvc.perform(get(ParameterClient.URL + ALERT))
                .andExpect(jsonPath("$.currentValue.value").value(15));
    }

    @Test
    void FR_015_K3_escalationNotGreaterThanReminder_isRejected() throws Exception {
        api.change("CFG-M15-06", api.etag("CFG-M15-06"), "{\"value\":20,\"unit\":\"HOURS\"}", "Thử nhỏ hơn mốc nhắc")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));
    }

    @Test
    void FR_014_missingReason_isRejectedBeforeAnyChange() throws Exception {
        int before = historyRows(ALERT);

        api.change(ALERT, api.etag(ALERT), MINUTES_20, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));

        assertThat(historyRows(ALERT)).isEqualTo(before);
    }

    @Test
    void US4_1_detail_listAndHistory_showCodeDescriptionValuesAndUsage() throws Exception {
        api.change(ALERT, api.etag(ALERT), MINUTES_20, "Để có lịch sử").andExpect(status().isOk());

        mvc.perform(get(ParameterClient.URL + ALERT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.type").value("DURATION"))
                .andExpect(jsonPath("$.unit").value("MINUTES"))
                .andExpect(jsonPath("$.usedIn").isNotEmpty())
                .andExpect(jsonPath("$.constraints.min").value(1));
        mvc.perform(get("/api/v1/parameters").param("module", "M05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'CFG-M05-01')]").exists())
                .andExpect(jsonPath("$[?(@.code == 'CFG-M15-05')]").doesNotExist());
        mvc.perform(get(ParameterClient.URL + ALERT + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actor.type").value("SCHEDULER"))
                .andExpect(jsonPath("$[0].effectiveTo").isNotEmpty())
                .andExpect(jsonPath("$[-1:].actor.id").value((int) MANAGER.accountId()))
                .andExpect(jsonPath("$[-1:].reason").value("Để có lịch sử"))
                .andExpect(jsonPath("$[-1:].effectiveTo").value((Object) null));
    }
}
