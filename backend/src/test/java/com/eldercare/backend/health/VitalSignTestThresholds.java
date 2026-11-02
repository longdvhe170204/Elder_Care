package com.eldercare.backend.health;

import com.eldercare.backend.support.TestUsers;
import org.springframework.jdbc.core.JdbcTemplate;

/** Ngưỡng minh họa chỉ cho các kịch bản test, không phải seed lâm sàng production. */
public final class VitalSignTestThresholds {
    private VitalSignTestThresholds() {}

    public static void seed(JdbcTemplate jdbc, long indicatorId) {
        jdbc.update("""
                INSERT INTO monitoring_setting(id,kind,scope,indicator_id,effective_from,status,components,created_by,created_at)
                VALUES(?,'THRESHOLD','FACILITY',?,'2020-01-01','EFFECTIVE',
                    '{"VALUE":{"lowerDangerous":90,"lowerWarning":94}}',1,'2020-01-01')
                """, TestUsers.nextId(), indicatorId);
    }
}
