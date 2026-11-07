package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.duty.service.DepartmentDutyService;
import com.eldercare.backend.staffing.internal.staff.service.StaffService;
import com.eldercare.backend.support.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/** T031/T033: lịch sử bộ phận, người phụ trách và giới hạn vai trò. */
@Import(DepartmentCommandsIT.Users.class)
class DepartmentCommandsIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    com.eldercare.backend.common.time.TestClockControl clock;

    @Autowired
    SwitchableUser current;

    @Autowired
    StaffService staff;

    @Autowired
    DepartmentDutyService duties;

    private final List<Long> departments = new ArrayList<>();
    private long first, second, person;

    @BeforeEach
    void setup() {
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        first = department();
        second = department();
        person = person(Role.NURSING_STAFF);
    }

    private long department() {
        long id = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.DEPARTMENT','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'leadStaffId',NULL) FROM (SELECT ? AS name) src",
                "[Bộ phận " + TestUsers.nextId() + "]");
        departments.add(id);
        return id;
    }

    private long person(Role role) {
        var u = new TestUsers(jdbc, time).staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên bộ phận]',?,?)",
                u.staffId(),
                role.name(),
                u.accountId());
        return u.staffId();
    }

    private void change(long person, long department, Long replacement) {
        ReflectionTestUtils.invokeMethod(
                staff, "changeDepartment", person, department, replacement, new Reason("Chuyển theo quyết định"));
    }

    private void lead(long department, long person) {
        ReflectionTestUtils.invokeMethod(
                duties, "assignLead", department, person, new Reason("Phân công phụ trách bộ phận"));
    }

    @AfterEach
    void clean() throws Exception {
        clock.reset();
        try (var c = retentionConnection()) {
            c.setAutoCommit(false);
            for (long id : departments)
                for (String statement : List.of(
                        "DELETE FROM status_history WHERE entity_type='M09.DEPARTMENT' AND attribute='LEAD_STAFF' AND entity_id=?",
                        "DELETE FROM staff_department WHERE department_id=(SELECT id FROM catalog_item WHERE catalog_type='M09.DEPARTMENT' AND COALESCE(m09_legacy_id,id)=?)",
                        "DELETE FROM catalog_item WHERE catalog_type='M09.DEPARTMENT' AND COALESCE(m09_legacy_id,id)=?"))
                    try (var p = c.prepareStatement(statement)) {
                        p.setLong(1, id);
                        p.executeUpdate();
                    }
            c.commit();
        }
    }

    @Test
    void DBR_36_changeClosesPreviousMembershipAndKeepsExactlyOneOpen() {
        change(person, first, null);
        change(person, second, null);
        assertThat(duties.departmentOf(person, time.now())).contains(second);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staff_department WHERE staff_id=? AND ends_at IS NULL",
                        Integer.class,
                        person))
                .isOne();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staff_department WHERE staff_id=? AND ends_at IS NOT NULL",
                        Integer.class,
                        person))
                .isOne();
    }

    @Test
    void Q_292_leadChangeNeedsReplacementInSameCommand() {
        change(person, first, null);
        lead(first, person);
        assertThatThrownBy(() -> change(person, second, null)).hasMessageContaining("người thay");
        assertThat(duties.lead(first)).contains(person);
        long replacement = person(Role.NURSING_STAFF);
        change(replacement, first, null);
        change(person, second, replacement);
        assertThat(duties.lead(first)).contains(replacement);
        assertThat(duties.departmentOf(person, time.now())).contains(second);
    }

    @Test
    void Q_293_managerCannotJoinDepartment() {
        long manager = person(Role.NURSING_HOME_MANAGER);
        assertThatThrownBy(() -> change(manager, first, null))
                .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
        assertThat(duties.departmentOf(manager, time.now())).isEmpty();
    }

    @Test
    void Q_292_leadMustBelongToDepartmentAndHavePermittedActiveAccount() {
        assertThatThrownBy(() -> lead(first, person))
                .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
        long accountant = person(Role.ACCOUNTANT);
        change(accountant, first, null);
        assertThatThrownBy(() -> lead(first, accountant))
                .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
        assertThat(duties.lead(first)).isEmpty();
    }

    @Test
    void FR_001a_nonManagerCannotChangeDepartment() {
        var ordinary = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        current.set(ordinary.user());
        assertThatThrownBy(() -> change(person, first, null))
                .isInstanceOf(com.eldercare.backend.common.error.PermissionDeniedException.class);
        assertThat(duties.departmentOf(person, time.now())).isEmpty();
    }

    @Test
    void FR_001a_departmentCreationUsesBusinessClock() {
        clock.setTo(java.time.Instant.parse("2033-03-01T00:00:00Z"));
        long id = ReflectionTestUtils.invokeMethod(duties, "create", "[Bộ phận đồng hồ " + person + "]", true);
        departments.add(id);
        assertThat(jdbc.queryForObject(
                                "SELECT created_at FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.legacy.id'),'null') AS UNSIGNED),c.id) AS id,c.name AS name,c.status AS status,c.version AS version,CAST(REPLACE(REPLACE(NULLIF(JSON_UNQUOTE(c.attributes->'$.createdAt'),'null'),'T',' '),'Z','') AS DATETIME(6)) AS created_at,CAST(NULLIF(JSON_UNQUOTE(c.attributes->'$.leadStaffId'),'null') AS UNSIGNED) AS lead_staff_id FROM catalog_item c WHERE c.catalog_type='M09.DEPARTMENT') department WHERE id=?",
                                java.sql.Timestamp.class,
                                id)
                        .toInstant())
                .isBetween(
                        java.time.Instant.parse("2033-03-01T00:00:00Z"),
                        java.time.Instant.parse("2033-03-01T00:01:00Z"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Users {
        @Bean
        @Primary
        SwitchableUser current() {
            return new SwitchableUser();
        }
    }
}
