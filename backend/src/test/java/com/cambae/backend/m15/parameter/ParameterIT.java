package com.cambae.backend.m15.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** DBR-24, FR-011, FR-018, FR-019 (000 T029 → T032): tham số nạp đủ, đọc lúc chạy, tra quá khứ. */
class ParameterIT extends MySqlIntegrationTestBase {

    @Autowired
    ParameterApi parameters;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_011_allCodesLoaded_eachWithInitialHistoryRow() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parameter", Integer.class))
                .isEqualTo(ParameterCode.values().length);
        // Lần nạp ban đầu: một dòng cho mỗi mã, người thực hiện là Bộ lập lịch (actor_id NULL). Test đổi tham số (T075)
        // thêm dòng lịch sử, nên chỉ đếm mã, không đếm dòng.
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(DISTINCT code) FROM parameter_history WHERE actor_id IS NULL", Integer.class))
                .isEqualTo(ParameterCode.values().length);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(DISTINCT p.code) FROM parameter p JOIN parameter_history h ON h.code = p.code"
                                + " WHERE JSON_CONTAINS(h.value, p.current_value) AND JSON_CONTAINS(p.current_value, h.value)",
                        Integer.class))
                .isEqualTo(ParameterCode.values().length);
    }

    @Test
    void FR_018_read_returnsCurrentValueByKind() {
        assertThat(parameters.read(ParameterCode.CFG_M15_05).asDuration()).isEqualTo(Duration.ofHours(48));
        assertThat(parameters.read(ParameterCode.CFG_M04_04).asInt()).isEqualTo(3);
        assertThat(parameters.read(ParameterCode.CFG_M11_02).asMoney()).isEqualTo(500_000L);
        assertThat(parameters.read(ParameterCode.CFG_M15_10).asTable(Map.class)).containsKey("RESIDENT_APP");
        assertThat(parameters.read(ParameterCode.CFG_M03_01).isSet()).isFalse();
    }

    @Test
    void FR_015_crossConstraint_isLoaded() {
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(constraints, '$.greaterThan')) FROM parameter WHERE code = 'CFG-M15-06'",
                        String.class))
                .isEqualTo("CFG-M15-05");
    }

    @Test
    void FR_019_readAt_usesHistory() {
        assertThat(parameters
                        .readAt(ParameterCode.CFG_M15_06, Instant.now().plusSeconds(60))
                        .asDuration())
                .isEqualTo(Duration.ofHours(96));
        assertThatThrownBy(() -> parameters.readAt(ParameterCode.CFG_M15_06, Instant.parse("2000-01-01T00:00:00Z")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void FR_011_appAccount_cannotAddOrRemoveCodes_norRewriteHistory() {
        assertThatThrownBy(() -> jdbc.update("DELETE FROM parameter WHERE code = 'CFG-M15-05'"))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
        assertThatThrownBy(() ->
                        jdbc.update("INSERT INTO parameter (code, description, kind, default_value, current_value)"
                                + " VALUES ('CFG-M99-01', 'x', 'INTEGER', '1', '1')"))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("INSERT command denied");
        assertThatThrownBy(() -> jdbc.update("UPDATE parameter_history SET reason = 'x'"))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("UPDATE command denied");
    }
}
