package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.internal.catalog.service.NonPerformanceReasonService;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** R-15 chuyển bảo vệ dòng cố định từ trigger bảng riêng sang service theo tasks T007a. */
@Transactional
@Import(FR_028a_ReasonCatalogHttpIT.Users.class)
class FR_028a_NonPerformanceReasonTypeIT extends MySqlIntegrationTestBase {
    @Autowired
    NonPerformanceReasonService service;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @BeforeEach
    void manager() {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
    }

    @Test
    void FR_028a_residentRefusalAlwaysActiveAndImmutableThroughCommands() {
        var fixed = service.list().stream().filter(r -> r.fixed()).findFirst().orElseThrow();
        assertThat(fixed.getName()).isEqualTo("Người cao tuổi từ chối");
        assertThat(fixed.getStatus().name()).isEqualTo("ACTIVE");
        assertThatThrownBy(() -> service.rename(fixed.getId(), "\"" + fixed.getVersion() + "\"", "TEST"))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("REASON_TYPE_FIXED"));
        assertThatThrownBy(() -> service.deactivate(fixed.getId(), Reason.of("Thử ngừng")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("REASON_TYPE_FIXED"));
        assertThat(service.list().stream().filter(r -> r.fixed()).findFirst().orElseThrow())
                .isEqualTo(fixed);
    }

    @Test
    void FR_028a_otherReasonsCanBeRenamedAndDeactivated() {
        var row = service.create("TEST_REASON_" + TestUsers.nextId());
        var renamed = service.rename(row.getId(), "\"0\"", row.getName() + "_REVISED");
        service.deactivate(renamed.getId(), Reason.of("Không còn dùng"));
        assertThat(service.list().stream()
                        .filter(r -> r.getId().equals(row.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getStatus()
                        .name())
                .isEqualTo("INACTIVE");
    }
}
