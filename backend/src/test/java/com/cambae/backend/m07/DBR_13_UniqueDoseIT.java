package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T019: Integration test verifying database triggers and unique constraints on dose tables
 * (V006_03__medication_dose.sql; DBR-13, BR-M07-12, FR-019, FR-022).
 */
class DBR_13_UniqueDoseIT extends MySqlIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("DBR-13: Triggers for medication_dose and dose_result exist in schema")
    void triggers_exist() throws SQLException {
        Set<String> triggers = new HashSet<>();
        try (Connection c = adminConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT LOWER(TRIGGER_NAME) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA = DATABASE()")) {
            while (rs.next()) {
                triggers.add(rs.getString(1));
            }
        }

        assertThat(triggers)
                .contains(
                        "trg_medication_dose_terminal_lock",
                        "trg_medication_dose_no_delete",
                        "trg_dose_result_no_update",
                        "trg_dose_result_no_delete");
    }
}
