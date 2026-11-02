package com.eldercare.backend.administration.parameter;

import static com.eldercare.backend.administration.parameter.ParameterFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * US4 #10; FR-022 (000 T078): đổi một dòng của tham số dạng bảng là một lần đổi cả tham số; lịch sử và nhật ký có đủ
 * nội dung bảng trước và sau.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(ParameterFixtures.class)
class ParameterTableIT extends MySqlIntegrationTestBase {

    /** Tham số dạng bảng còn lại: CFG-M02-05 không còn là bảng từ Q-395 (V004_06). */
    private static final String COVERAGE_TABLE = "CFG-M09-07";

    @Autowired
    MockMvc mvc;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JsonMapper json;

    private ParameterClient api;

    @BeforeEach
    void setUp() {
        api = new ParameterClient(mvc);
        user.set(MANAGER);
    }

    @AfterEach
    void restore() throws Exception {
        api.restoreDefault(COVERAGE_TABLE, api.etag(COVERAGE_TABLE), "khôi phục sau test");
    }

    @Test
    void US4_10_changingOneRow_logsWholeTableBeforeAndAfter() throws Exception {
        JsonNode before = json.readTree(jdbc.queryForObject(
                "SELECT current_value FROM parameter WHERE code = ?", String.class, COVERAGE_TABLE));
        ObjectNode after = (ObjectNode) before.deepCopy();
        ((ObjectNode) after.get("rows").get(0)).put("minCount", 2);

        api.change(COVERAGE_TABLE, api.etag(COVERAGE_TABLE), json.writeValueAsString(after), "Tăng phủ ca tối thiểu")
                .andExpect(status().isOk());

        JsonNode history = json.readTree(jdbc.queryForObject(
                "SELECT value FROM parameter_history WHERE code = ? ORDER BY effective_from DESC LIMIT 1",
                String.class,
                COVERAGE_TABLE));
        assertThat(history).isEqualTo(after);
        JsonNode logBefore = json.readTree(jdbc.queryForObject(
                "SELECT JSON_EXTRACT(data_before, '$.value') FROM audit_log WHERE action = 'PARAMETER_CHANGED'"
                        + " AND entity_id = ? ORDER BY id DESC LIMIT 1",
                String.class,
                COVERAGE_TABLE));
        JsonNode logAfter = json.readTree(jdbc.queryForObject(
                "SELECT JSON_EXTRACT(data_after, '$.value') FROM audit_log WHERE action = 'PARAMETER_CHANGED'"
                        + " AND entity_id = ? ORDER BY id DESC LIMIT 1",
                String.class,
                COVERAGE_TABLE));
        assertThat(logBefore).isEqualTo(before);
        assertThat(logAfter).isEqualTo(after);
        assertThat(logAfter.get("rows")).hasSize(before.get("rows").size());
    }
}
