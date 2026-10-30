package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class FR_028a_NonPerformanceReasonTypeIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_028a_residentRefusalAlwaysActiveAndImmutableInDatabase() {
        var fixed = jdbc.queryForMap("SELECT id,name,status FROM care_non_performance_reason_type WHERE fixed=true");
        long id = ((Number) fixed.get("id")).longValue();
        assertThat(fixed).containsEntry("name", "Người cao tuổi từ chối").containsEntry("status", "ACTIVE");
        assertThatThrownBy(() -> jdbc.update("UPDATE care_non_performance_reason_type SET name='TEST' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE care_non_performance_reason_type SET fixed=false WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE care_non_performance_reason_type SET status='INACTIVE' WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM care_non_performance_reason_type WHERE id=?", id))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_028a_otherReasonsCanBeRenamedAndDeactivated() {
        jdbc.update("INSERT INTO care_non_performance_reason_type(name,fixed) VALUES ('TEST_REASON',false)");
        jdbc.update(
                "UPDATE care_non_performance_reason_type SET name='TEST_REASON_REVISED',status='INACTIVE' WHERE name='TEST_REASON'");
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM care_non_performance_reason_type WHERE name='TEST_REASON_REVISED'",
                        String.class))
                .isEqualTo("INACTIVE");
    }
}
