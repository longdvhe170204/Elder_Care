package com.cambae.backend.m15.auditlog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import com.cambae.backend.m15.auditlog.api.spi.FieldClassificationProvider;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * FR-049, US3 #7 (000 contracts/module-interfaces.md mục 1.3; 002 mục 1.9): với quyền và phạm vi thật, nhân viên có
 * quyền "Nhật ký thay đổi" theo phân công (P) xem được lịch sử đối tượng thuộc người cao tuổi mình được phân công; phạm
 * vi của đối tượng lấy từ {@code ScopeAssignmentProvider} của loại đó. Loại không có provider phạm vi thì chỉ Quản lý
 * viện xem (mặc định an toàn).
 */
@AutoConfigureMockMvc
@Import({FakeM15Config.class, FR_049_AuditHistoryAssignedScopeIT.Providers.class})
class FR_049_AuditHistoryAssignedScopeIT extends MySqlIntegrationTestBase {

    static final String SCOPED = "M15.SAMPLE_SCOPED_RECORD";
    static final String NO_SCOPE_PROVIDER = "M15.SAMPLE_NO_SCOPE_RECORD";

    /** Người cao tuổi của từng bản ghi mẫu, provider phạm vi đọc ở đây. */
    static final Map<Long, Long> RESIDENT_OF = new ConcurrentHashMap<>();

    @TestConfiguration
    static class Providers {

        @Bean
        FieldClassificationProvider scopedFields() {
            return fields(SCOPED);
        }

        @Bean
        FieldClassificationProvider noScopeFields() {
            return fields(NO_SCOPE_PROVIDER);
        }

        @Bean
        ScopeAssignmentProvider scopedScope() {
            return new ScopeAssignmentProvider() {
                @Override
                public String entityType() {
                    return SCOPED;
                }

                @Override
                public Target assignScope(long id) {
                    return new Target(SCOPED, String.valueOf(id), RESIDENT_OF.get(id), null);
                }
            };
        }

        private static FieldClassificationProvider fields(String type) {
            return new FieldClassificationProvider() {
                @Override
                public String entityType() {
                    return type;
                }

                @Override
                public Map<String, InfoGroup> fieldGroups() {
                    return Map.of("note", InfoGroup.INTERNAL_PUBLIC);
                }
            };
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeAssignments assignments;

    private void audit(String type, long id, Long residentId) {
        jdbc.update(
                "INSERT INTO audit_log(occurred_at,actor_type,actor_id,role,action,entity_type,entity_id,resident_id,data_after)"
                        + " VALUES (?,'SCHEDULER',NULL,'SCHEDULER','SAMPLE_NOTE_ADDED',?,?,?,JSON_OBJECT('note','ghi chú'))",
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC),
                type,
                String.valueOf(id),
                residentId);
    }

    private org.springframework.test.web.servlet.ResultActions history(TestUser as, String type, long id)
            throws Exception {
        return mvc.perform(get("/api/v1/audit-logs/entities/" + type + "/" + id)
                .header("Authorization", TestUsers.bearer(new TestUsers(jdbc, time).session(as.accountId()))));
    }

    @Test
    void FR_049_assignedNurse_seesHistoryOfOwnResident_notOthers() throws Exception {
        TestUsers users = new TestUsers(jdbc, time);
        TestUser nurse = users.staff(Role.NURSING_STAFF);
        long resident = TestUsers.nextId(), other = TestUsers.nextId();
        long mine = TestUsers.nextId(), notMine = TestUsers.nextId();
        RESIDENT_OF.put(mine, resident);
        RESIDENT_OF.put(notMine, other);
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        resident,
                        TestUsers.nextId(),
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(7))));
        audit(SCOPED, mine, resident);
        audit(SCOPED, notMine, other);

        history(nurse, SCOPED, mine)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
        history(nurse, SCOPED, notMine).andExpect(status().isNotFound());
    }

    @Test
    void FR_049_typeWithoutScopeProvider_onlyManagerSeesHistory() throws Exception {
        TestUsers users = new TestUsers(jdbc, time);
        TestUser nurse = users.staff(Role.NURSING_STAFF);
        TestUser manager = users.staff(Role.NURSING_HOME_MANAGER);
        long resident = TestUsers.nextId(), id = TestUsers.nextId();
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        resident,
                        TestUsers.nextId(),
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(7))));
        audit(NO_SCOPE_PROVIDER, id, resident);

        history(nurse, NO_SCOPE_PROVIDER, id).andExpect(status().isNotFound());
        history(manager, NO_SCOPE_PROVIDER, id).andExpect(status().isOk());
    }
}
