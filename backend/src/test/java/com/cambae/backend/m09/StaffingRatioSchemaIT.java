package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class StaffingRatioSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    java.util.List<com.cambae.backend.m15.retention.api.spi.RetentionProvider> providers;

    @Test
    void FR_037_calculationHasRetentionProvider() {
        assertThat(providers).anyMatch(p -> p.dataType().equals("M09.STAFFING_RATIO_CALCULATION"));
    }

    @Test
    void FR_037_calculationsAppendOnly_withTenTriggerTypesAndDecimalWeight() {
        long shift = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO staffing_ratio_calculation(shift_id,calculated_at,event,total_weight,staff_count,ratio,threshold,meets_threshold) VALUES (?, '2026-10-25 00:00:00','DRAFT_SAVED',35.00,4,8.75,8.00,FALSE)",
                shift);
        assertThat(jdbc.queryForObject(
                        "SELECT total_weight FROM staffing_ratio_calculation WHERE shift_id=?",
                        java.math.BigDecimal.class,
                        shift))
                .isEqualByComparingTo("35.00");
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE staffing_ratio_calculation SET staff_count=8 WHERE shift_id=?", shift))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM staffing_ratio_calculation WHERE shift_id=?", shift))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO staffing_ratio_calculation(shift_id,calculated_at,event,total_weight,staff_count,ratio,threshold,meets_threshold) VALUES (?, '2026-10-25 00:00:00','UNKNOWN',1,1,1,8,TRUE)",
                        shift))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        var type = jdbc.queryForObject(
                "SELECT column_type FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='staffing_ratio_calculation' AND column_name='event'",
                String.class);
        assertThat(type.split(",")).hasSize(10);
    }
}
