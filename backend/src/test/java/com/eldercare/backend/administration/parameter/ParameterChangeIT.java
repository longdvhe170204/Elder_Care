package com.eldercare.backend.administration.parameter;

import static com.eldercare.backend.administration.parameter.ParameterFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.TestClockControl;
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

    @Autowired
    TestClockControl clock;

    private ParameterClient api;

    @BeforeEach
    void setUp() {
        api = new ParameterClient(mvc);
        user.set(MANAGER);
    }

    @AfterEach
    void restore() throws Exception {
        clock.reset();
        user.set(MANAGER);
        api.restoreDefault(ALERT, api.etag(ALERT), "khôi phục sau test");
    }

    @Test
    void US4_7_restoreDefault_isAnOrdinaryChange_markedAsRestore() throws Exception {
        api.change(ALERT, api.etag(ALERT), MINUTES_20, "Thử giá trị mới").andExpect(status().isOk());

        api.restoreDefault(ALERT, api.etag(ALERT), "Trở về mặc định")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue.value").value(15));

        Map<String, Object> last = jdbc.queryForMap(
                "SELECT command, reason, JSON_EXTRACT(details, '$.value.value') AS v FROM status_history"
                        + " WHERE entity_type = 'M15.PARAMETER' AND entity_id = 0 AND attribute = ? ORDER BY starts_at DESC LIMIT 1",
                ALERT);
        assertThat(last).containsEntry("command", "RESTORE_DEFAULT").containsEntry("reason", "Trở về mặc định");
        assertThat(String.valueOf(last.get("v"))).isEqualTo("15");
    }
}
