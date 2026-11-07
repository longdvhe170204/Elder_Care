package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
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
        long staff = TestUsers.nextId();
        jdbc.update("INSERT INTO staff_profile(id,full_name,position) VALUES (?,'[Lưu giữ ca]','Điều dưỡng')", staff);
        jdbc.update(
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,src.status,JSON_OBJECT('createdAt',DATE_FORMAT(src.created_at,'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(TRUE,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Lưu giữ ca] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,'INACTIVE' AS status,'2000-01-01' AS created_at) src");
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
                "INSERT INTO status_history(entity_type,entity_id,attribute,module,kind,value_id,starts_at,ends_at,command,by_system) SELECT 'M09.SHIFT',src.shift_id,'LEAD_STAFF','M09','PERIOD',src.staff_id,src.starts_at,src.ends_at,src.basis,FALSE FROM (SELECT ? AS shift_id,? AS staff_id,'2010-01-01' AS starts_at,'2010-01-01 08:00' AS ends_at,'SCHEDULED' AS basis) src",
                shift,
                staff);
        long lead = id();
        jdbc.update(
                "INSERT INTO record_event(entity_type,entity_id,module,kind,by_system,occurred_at,confirmed_at,content) SELECT 'M09.SCHEDULE',src.schedule_id,'M09','M09.SHIFT_PUBLICATION',FALSE,src.published_at,src.published_at,JSON_OBJECT('content',CAST(src.content AS JSON)) FROM (SELECT ? AS schedule_id,'2010-01-01' AS published_at,'{}' AS content) src",
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
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(TRUE,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Ca còn dùng] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type) src");
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
