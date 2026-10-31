package com.eldercare.backend.care;

import static com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds.*;
import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.care.internal.catalog.service.CareCatalogService;
import com.eldercare.backend.care.internal.scheduling.persistence.CareMilestoneStore;
import com.eldercare.backend.care.internal.status.service.CareResidentLock;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Cùng bảo đảm T005 sau R-15: enum/chứng chỉ kiểm ở API,duy nhất vẫn ở bảng chung. */
@Transactional
@Import(FR_028a_ReasonCatalogHttpIT.Users.class)
class T005_FoundationSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CareCatalogService service;

    @Autowired
    CareResidentLock locks;

    @Autowired
    CareMilestoneStore milestones;

    @Autowired
    SwitchableUser user;

    @Autowired
    BusinessTime time;

    @BeforeEach
    void manager() {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
    }

    private TaskTypeAttributes attributes(String group) {
        return new TaskTypeAttributes(
                group,
                false,
                "PERFORMED",
                Role.CAREGIVER,
                "NORMAL",
                null,
                null,
                false,
                null,
                null,
                null,
                Set.of("TEST"),
                null);
    }

    @Test
    void FR_038_activeNameIsUniqueAndInactiveNameCanBeReused() {
        String name = "TEST_SCHEMA_" + TestUsers.nextId();
        var row = service.createTaskType(name, attributes("OTHER"));
        assertThatThrownBy(() -> service.createTaskType(name, attributes("OTHER")))
                .isInstanceOf(BusinessException.class);
        service.deactivateTaskType(row.id(), Reason.of("Đổi danh mục"));
        service.createTaskType(name, attributes("OTHER"));
        assertThat(service.taskTypes().stream().filter(r -> r.name().equals(name)))
                .hasSize(2);
    }

    @Test
    void FR_038_allEighteenGroupsAndNullableCfgWindowsAreStored() {
        List<String> groups = List.of(
                "PERSONAL_HYGIENE",
                "BATHING",
                "DRESSING",
                "EATING_ASSISTANCE",
                "DRINKING_ASSISTANCE",
                "TOILETING_ASSISTANCE",
                "MOVEMENT",
                "REHABILITATION",
                "VITAL_SIGN_MEASUREMENT",
                "ACTIVITY",
                "SLEEP",
                "MOOD",
                "BEHAVIOR",
                "DIAPER_ITEM_CHANGE",
                "SHIFT_START_CHECK",
                "SUPERVISION",
                "MEDICATION",
                "OTHER");
        for (String group : groups) {
            var row = service.createTaskType("TEST_SCHEMA_" + group + TestUsers.nextId(), attributes(group));
            assertThat(row.attributes().group()).isEqualTo(group);
            assertThat(row.attributes().windowBeforeMinutes()).isNull();
            assertThat(row.attributes().windowAfterMinutes()).isNull();
            assertThat(row.attributes().certificates()).containsExactly("TEST");
        }
        assertThatThrownBy(() -> service.createTaskType("INVALID_" + TestUsers.nextId(), attributes("INVALID")))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> service.updateTaskType(Long.MAX_VALUE, "\"0\"", "Không có loại", attributes("OTHER")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void NFR_04_technicalKeysAndResultValueCatalogArePersisted() {
        long id = TestUsers.nextId();
        locks.lock(List.of(id));
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_EXTRACT(data,'$.version') FROM resident_module_state WHERE module='M04' AND resident_id=?",
                        Integer.class,
                        id))
                .isZero();
        String job = "TEST_" + id;
        assertThat(milestones.claim(job, LocalDate.of(2026, 10, 31))).isTrue();
        assertThat(milestones.claim(job, LocalDate.of(2026, 10, 31))).isFalse();
        var result = service.createResultValue(
                "TEST_SLEEP_" + id, new ResultValueAttributes("SLEEP_QUALITY", "TEST", "INSOMNIA", null));
        assertThat(result.attributes().ruleGroup()).isEqualTo("INSOMNIA");
    }
}
