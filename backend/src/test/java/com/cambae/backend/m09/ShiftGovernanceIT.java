package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** T009/T012: đăng ký cùng migration, giữ ca đang dùng và gỡ đồ thị phụ thuộc bằng tài khoản lưu giữ. */
class ShiftGovernanceIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    RetentionDatabase database;

    private RetentionProvider provider(String type) {
        return retention.stream()
                .filter(p -> p.dataType().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private long id() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void FR_051_closedShiftGraphHasScopeReferencesAndCanBePurgedInDependencyOrder() {
        long floor = TestUsers.nextId();
        jdbc.update("INSERT INTO staff_profile(full_name,position) VALUES ('[Lưu giữ ca]','Điều dưỡng')");
        long staff = id();
        jdbc.update(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,status,created_at) VALUES ('[Lưu giữ ca]','07:00','15:00','FLOOR','INACTIVE','2000-01-01')");
        long template = id();
        jdbc.update(
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status,created_at) VALUES ('2010-01','FLOOR',?,'PUBLISHED','2000-01-01')",
                floor);
        long schedule = id();
        jdbc.update(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status,lead_staff_id) VALUES (?,?,'FLOOR',?,'2010-01-01','2010-01-01 08:00',TRUE,'CLOSED',?)",
                schedule,
                template,
                floor,
                staff);
        long shift = id();
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, staff);
        long member = id();
        jdbc.update(
                "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,ends_at,basis) VALUES (?,?,'2010-01-01','2010-01-01 08:00','SCHEDULED')",
                shift,
                staff);
        long lead = id();
        jdbc.update(
                "INSERT INTO shift_publication(schedule_id,published_at,content) VALUES (?,'2010-01-01','{}')",
                schedule);
        long publication = id();
        assertThat(scopes.stream()
                        .filter(s -> s.entityType().equals("M09.SHIFT"))
                        .findFirst()
                        .orElseThrow()
                        .assignScope(shift)
                        .floorAreaId())
                .isEqualTo(floor);
        assertThat(provider("M09.STAFF").referencesTo(staff))
                .contains(
                        new RetentionProvider.DataRef("M09.SHIFT_STAFF", member),
                        new RetentionProvider.DataRef("M09.SHIFT_LEAD_HISTORY", lead),
                        new RetentionProvider.DataRef("M09.SHIFT", shift));
        assertThat(provider("M09.SHIFT").referencesTo(shift))
                .contains(
                        new RetentionProvider.DataRef("M09.SHIFT_STAFF", member),
                        new RetentionProvider.DataRef("M09.SHIFT_LEAD_HISTORY", lead));
        var cutoff = Instant.parse("2020-01-01T00:00:00Z");
        String[] types = {
            "M09.SHIFT_STAFF",
            "M09.SHIFT_LEAD_HISTORY",
            "M09.SHIFT_PUBLICATION",
            "M09.SHIFT",
            "M09.SCHEDULE",
            "M09.SHIFT_TEMPLATE"
        };
        long[] ids = {member, lead, publication, shift, schedule, template};
        for (int i = 0; i < types.length; i++) {
            var p = provider(types[i]);
            long row = ids[i];
            assertThat(p.expiredCandidates(cutoff, 100)).anyMatch(c -> c.id() == row);
            database.transaction().executeWithoutResult(s -> p.delete(List.of(row)));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM work_shift WHERE id=?", Integer.class, shift))
                .isZero();
    }

    @Test
    void FR_051_oldButUnfinishedShiftIsNotAPurgeCandidate() {
        long floor = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type) VALUES ('[Ca còn dùng]','07:00','15:00','FLOOR')");
        long template = id();
        jdbc.update("INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('2010-01','FLOOR',?)", floor);
        long schedule = id();
        jdbc.update(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover,status) VALUES (?,?,'FLOOR',?,'2010-01-01','2010-01-01 08:00',TRUE,'AWAITING_HANDOVER')",
                schedule,
                template,
                floor);
        long shift = id();
        assertThat(provider("M09.SHIFT").expiredCandidates(Instant.parse("2020-01-01T00:00:00Z"), 100))
                .noneMatch(c -> c.id() == shift);
    }
}
