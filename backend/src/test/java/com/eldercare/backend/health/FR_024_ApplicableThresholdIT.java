package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.health.internal.threshold.service.ApplicableThresholdResolver;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** FR-024: nguồn ngưỡng thật phải theo phiên bản và thời điểm đo, không theo tên chỉ số. */
@Transactional
class FR_024_ApplicableThresholdIT extends MySqlIntegrationTestBase {
    private static final Instant AT = Instant.parse("2026-11-02T08:00:00Z");

    @Autowired
    ApplicableThresholdResolver resolver;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_024_personalVersionWinsOnlyDuringItsEffectiveInterval() {
        long resident = TestUsers.nextId();
        long indicator = TestUsers.nextId();
        long facility = threshold(null, indicator, AT.minusSeconds(3600), null, "EFFECTIVE", 91);
        long personal = threshold(resident, indicator, AT, AT.plusSeconds(60), "EXPIRED", 85);
        threshold(resident, indicator, AT.minusSeconds(60), null, "CANCELLED", 99);
        assertThat(resolver.resolve(resident, indicator, "ARBITRARY", AT.minusSeconds(1))
                        .thresholdVersionId())
                .isEqualTo(facility);
        var at = resolver.resolve(resident, indicator, "ARBITRARY", AT);
        assertThat(at.thresholdVersionId()).isEqualTo(personal);
        assertThat(at.bands().get("VALUE").lowerDangerous()).isEqualByComparingTo("85");
        assertThat(resolver.resolve(resident, indicator, "ARBITRARY", AT.plusSeconds(60))
                        .thresholdVersionId())
                .isEqualTo(facility);
        assertThat(resolver.resolve(TestUsers.nextId(), indicator, "ARBITRARY", AT)
                        .thresholdVersionId())
                .isEqualTo(facility);
    }

    private long threshold(Long resident, long indicator, Instant from, Instant to, String status, int danger) {
        long id = TestUsers.nextId();
        jdbc.update(
                """
                INSERT INTO monitoring_setting(id,kind,scope,resident_id,indicator_id,effective_from,effective_until,
                    status,components,created_by,created_at)
                VALUES(?,'THRESHOLD',?,?,?,?,?,?,CAST(? AS JSON),1,?)
                """,
                id,
                resident == null ? "FACILITY" : "PERSONAL",
                resident,
                indicator,
                Timestamp.from(from),
                to == null ? null : Timestamp.from(to),
                status,
                "{\"VALUE\":{\"lowerDangerous\":" + danger + ",\"lowerWarning\":95}}",
                Timestamp.from(AT));
        return id;
    }
}
