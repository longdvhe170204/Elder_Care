package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Nguồn thật M06: xác nhận, đính chính và thời điểm đo; vị trí dùng bản giả chung của C. */
@Transactional
@Import(LatestVitalSignReadIT.Locations.class)
class LatestVitalSignReadIT extends MySqlIntegrationTestBase {
    private static final Instant FROM = Instant.parse("2026-10-30T08:00:00Z");
    private static final Instant TO = FROM.plusSeconds(3600);

    @Autowired
    VitalSignApi api;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FakeLocations locations;

    @Test
    void FR_030_currentCorrectionAndVoidFallBackWithoutMutatingOriginal() {
        long resident = TestUsers.nextId();
        long indicator = indicator(true);
        long older = record(resident, indicator, FROM, "CONFIRMED", "NORMAL", "71.25");
        long newer = record(resident, indicator, FROM.plusSeconds(60), "CONFIRMED", "NORMAL", "72.50");
        amend(newer, "{\"values\":{\"VALUE\":74.35}}");
        assertThat(api.latestPerType(resident).get(indicator).values().get("VALUE"))
                .isEqualByComparingTo(new BigDecimal("74.35"));
        assertThat(new BigDecimal(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(components,'$.VALUE.value')) FROM vital_sign_record WHERE id=?",
                        String.class,
                        newer)))
                .isEqualByComparingTo("72.50");
        jdbc.update(
                "INSERT INTO correction(record_type,original_record_id,type,reason,actor_id,creation_method,recorded_at,sequence_no) VALUES('M06.VITAL_SIGN_RECORD',?,'VOID','Hủy bản ghi kiểm thử',1,'A',?,2)",
                newer,
                Timestamp.from(TO));
        jdbc.update(
                "UPDATE correction_chain SET voided=TRUE WHERE record_type='M06.VITAL_SIGN_RECORD' AND original_record_id=?",
                newer);
        assertThat(api.latestPerType(resident).get(indicator).recordId()).isEqualTo(older);
    }

    private long indicator(boolean vitalSign) {
        long suffix = TestUsers.nextId();
        return catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "TEST_" + suffix,
                                "Chỉ số kiểm thử " + suffix,
                                null,
                                0,
                                Map.of("unit", "đơn vị kiểm thử", "vitalSign", vitalSign)))
                .id();
    }

    private long record(long resident, long indicator, Instant measuredAt, String status, String level, String value) {
        long id = TestUsers.nextId();
        Timestamp confirmed = status.equals("AWAITING_REMEASUREMENT") ? null : Timestamp.from(TO);
        jdbc.update(
                """
                INSERT INTO vital_sign_record(id,resident_id,indicator_id,measured_at,measured_by,
                  level,confirmation_status,components,labels,confirmed_at,locked_at,created_at,created_by)
                VALUES(?,?,?,?,?,?,?,CAST(? AS JSON),'',?,?,?,?)
                """,
                id,
                resident,
                indicator,
                Timestamp.from(measuredAt),
                1L,
                level,
                status,
                "{\"VALUE\":{\"value\":" + value + ",\"level\":\"" + level + "\"}}",
                confirmed,
                confirmed,
                Timestamp.from(TO),
                1L);
        return id;
    }

    private void amend(long id, String content) {
        jdbc.update("""
                INSERT INTO correction(record_type,original_record_id,type,corrected_content,reason,actor_id,
                  creation_method,recorded_at,sequence_no)
                VALUES('M06.VITAL_SIGN_RECORD',?,'AMEND_CONTENT',?,'Sửa giá trị kiểm thử',1,'A',?,1)
                """, id, content, Timestamp.from(TO));
        jdbc.update(
                "INSERT INTO correction_chain(record_type,original_record_id,correction_count,voided) VALUES('M06.VITAL_SIGN_RECORD',?,1,FALSE)",
                id);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Locations {
        @Bean
        @Primary
        FakeLocations locations() {
            return new FakeLocations();
        }
    }
}
