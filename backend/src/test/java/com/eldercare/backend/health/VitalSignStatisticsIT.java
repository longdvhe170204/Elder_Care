package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.health.api.VitalSignStatisticsApi;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.dto.VitalSignReportView;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Nguồn thật cho 016: thống kê lần xác nhận, giữ mức lúc đo và lọc phạm vi tại lúc đo. */
@Transactional
@Import(VitalSignStatisticsIT.Locations.class)
class VitalSignStatisticsIT extends MySqlIntegrationTestBase {
    @Autowired
    VitalSignStatisticsApi api;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FakeLocations locations;

    private final Instant from = Instant.parse("2026-11-01T01:00:00Z");
    private final Instant to = from.plusSeconds(3600);

    @Test
    void FR_081_confirmedOnlyExcludesParentAwaitingVoidedAndUpperBoundary() {
        long resident = TestUsers.nextId();
        long indicator = indicator();
        long valid = record(resident, indicator, from, "CONFIRMED", "NOT_REMEASURED");
        record(resident, indicator, from, "REMEASURED", "");
        record(resident, indicator, from, "AWAITING_REMEASUREMENT", "");
        record(resident, indicator, to, "CONFIRMED", "");
        long voided = record(resident, indicator, from.plusSeconds(1), "CONFIRMED", "");
        jdbc.update(
                "INSERT INTO correction_chain(record_type,original_record_id,correction_count,voided) VALUES('M06.VITAL_SIGN_RECORD',?,1,TRUE)",
                voided);
        var scope = new ScopeFilter(false, Set.of(), Set.of(), Set.of(resident));
        assertThat(api.readings(scope, from, to)).singleElement().satisfies(row -> {
            assertThat(row.recordId()).isEqualTo(valid);
            assertThat(row.level()).isEqualTo(VitalSignLevel.WARNING);
        });
    }

    @Test
    void FR_081_scopeUsesHistoricalLocationAndEmptyScopeRevealsNothing() {
        long resident = TestUsers.nextId();
        long indicator = indicator();
        long floor = TestUsers.nextId();
        long room = TestUsers.nextId();
        locations.floor(floor, null).room(room, floor).stay(resident, room, from, to);
        long id = record(resident, indicator, from, "CONFIRMED", "");
        var floorScope = new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
        var roomScope = new ScopeFilter(false, Set.of(), Set.of(room), Set.of());
        assertThat(api.readings(floorScope, from, to))
                .extracting(VitalSignReportView::recordId)
                .containsExactly(id);
        assertThat(api.readings(roomScope, from, to))
                .extracting(VitalSignReportView::recordId)
                .containsExactly(id);
        assertThat(api.readings(new ScopeFilter(false, Set.of(), Set.of(), Set.of()), from, to))
                .isEmpty();
    }

    private long indicator() {
        long suffix = TestUsers.nextId();
        return catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "STAT_" + suffix,
                                "Chỉ số kiểm thử " + suffix,
                                null,
                                0,
                                Map.of("unit", "đơn vị kiểm thử", "vitalSign", false)))
                .id();
    }

    private long record(long resident, long indicator, Instant at, String status, String labels) {
        long id = TestUsers.nextId();
        Timestamp confirmed = status.equals("AWAITING_REMEASUREMENT") ? null : Timestamp.from(to);
        jdbc.update(
                """
            INSERT INTO vital_sign_record(id,resident_id,indicator_id,measured_at,measured_by,level,
            confirmation_status,components,labels,confirmed_at,locked_at,created_at,created_by)
            VALUES(?,?,?,?,1,'WARNING',?,CAST(? AS JSON),?,?,?,?,1)
            """,
                id,
                resident,
                indicator,
                Timestamp.from(at),
                status,
                "{\"VALUE\":{\"value\":1,\"level\":\"WARNING\"}}",
                labels,
                confirmed,
                confirmed,
                Timestamp.from(to));
        return id;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Locations {
        @Bean
        @Primary
        FakeLocations statisticsLocations() {
            return new FakeLocations();
        }
    }
}
