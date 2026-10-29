package com.eldercare.backend.administration.retention;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.scheduling.ExpiredDataPurgeJob;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FR-050, FR-051a, BR-M15-12 (000 plan D5; báo lỗi của B, 001 T011): nhật ký gắn hồ sơ người cao tuổi đi cùng một bản ghi
 * tính hạn theo hồ sơ (CFG-M15-03) được loại bỏ cùng chuỗi khi bản ghi đó đã hết hạn và chính dòng nhật ký đã qua
 * CFG-M15-04; dòng nhật ký chưa qua CFG-M15-04 thì giữ cả chuỗi. Bản ghi mẫu dùng bảng {@code sample_floor_record}, mốc
 * tính hạn là {@code occurred_at}.
 */
@Import(FR_050_ResidentLinkedLogRetentionIT.Fixtures.class)
class FR_050_ResidentLinkedLogRetentionIT extends MySqlIntegrationTestBase {

    static final String TYPE = "M15.SAMPLE_RESIDENT_LINKED";
    static final CurrentUser SCHEDULER = FakePermissions.user(0, Role.SCHEDULER);

    @TestConfiguration
    static class Fixtures {

        @Bean
        SwitchableUser currentUser() {
            return new SwitchableUser();
        }

        @Bean
        RetentionProvider residentLinkedRetention(JdbcTemplate jdbc, RetentionDatabase database) {
            return new RetentionProvider() {
                @Override
                public String dataType() {
                    return TYPE;
                }

                @Override
                public RetentionKind retentionKind() {
                    return new RetentionKind.ResidentRecord();
                }

                @Override
                public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
                    return jdbc
                            .query(
                                    "SELECT id, occurred_at FROM sample_floor_record WHERE occurred_at < ? ORDER BY id LIMIT ?",
                                    (rs, i) -> new Candidate(
                                            rs.getLong(1),
                                            rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                                    LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                                    batch)
                            .stream();
                }

                @Override
                public Set<DataRef> referencesTo(long id) {
                    return Set.of();
                }

                @Override
                public void delete(Collection<Long> ids) {
                    String in = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                    database.jdbc().update("DELETE FROM sample_floor_record WHERE id IN (" + in + ")");
                }
            };
        }
    }

    @Autowired
    ExpiredDataPurgeJob job;

    @Autowired
    SwitchableUser user;

    @Autowired
    BusinessTime time;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void reset() {
        user.set(SCHEDULER);
    }

    private Instant yearsAgo(int years) {
        return time.now().atZone(BusinessTime.ZONE).minusYears(years).toInstant();
    }

    private long record(Instant at) {
        jdbc.update(
                "INSERT INTO sample_floor_record (occurred_at, actor_id, floor_area_id, note) VALUES (?, 1, 2, 'ghi nhận')",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC));
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void log(long id, Instant at, long residentId) {
        jdbc.update(
                "INSERT INTO audit_log(occurred_at,actor_type,actor_id,role,action,entity_type,entity_id,resident_id)"
                        + " VALUES (?,'SCHEDULER',NULL,'SCHEDULER','SAMPLE_RECORDED',?,?,?)",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                TYPE,
                String.valueOf(id),
                residentId);
    }

    private boolean exists(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sample_floor_record WHERE id = ?", Integer.class, id) > 0;
    }

    private int logs(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = ? AND entity_id = ?",
                Integer.class,
                TYPE,
                String.valueOf(id));
    }

    @Test
    void FR_050_residentRecordExpired_itsResidentLogPastCfgM1504_isPurgedWithChain() {
        long resident = TestUsers.nextId();
        long id = record(yearsAgo(11));
        log(id, yearsAgo(11), resident);
        user.set(SCHEDULER);

        job.run();

        assertThat(exists(id)).isFalse();
        assertThat(logs(id)).isZero();
    }

    @Test
    void FR_050_residentLogStillWithinCfgM1504_keepsWholeChain() {
        long resident = TestUsers.nextId();
        long id = record(yearsAgo(11));
        log(id, time.now().minusSeconds(86_400), resident);
        user.set(SCHEDULER);

        job.run();

        assertThat(exists(id)).isTrue();
        assertThat(logs(id)).isEqualTo(1);
    }
}
