package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.incident.internal.alert.domain.*;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** T006: kiểm API bảng chung thật, quyền thật, nhật ký và rollback trên MySQL. */
@Import(FR_039_IncidentCatalogIT.Users.class)
class FR_039_IncidentCatalogIT extends MySqlIntegrationTestBase {
    @Autowired
    com.eldercare.backend.health.internal.catalog.service.VitalSignCatalogService vitalSignCatalog;

    @Test
    void FR_002_vitalSignMetadataComponentsAndAuditUseOneTransaction() {
        String code = "VITAL_" + TestUsers.nextId();
        var input = new com.eldercare.backend.health.internal.catalog.domain.VitalSignCatalogInput(
                code,
                code,
                "đơn vị kiểm thử",
                true,
                java.util.List.of(new com.eldercare.backend.health.internal.catalog.domain.VitalSignComponentDefinition(
                        "VALUE", "Giá trị", 1)));
        var created = vitalSignCatalog.create(input);
        assertThat(created.components()).hasSize(1);
        assertThat(created.missingValidRange()).isTrue();
        var stopped = vitalSignCatalog.deactivate(created.id(), new Reason("Ngừng chỉ số kiểm thử"));
        assertThat(stopped.active()).isFalse();
        assertThat(catalogs.list(
                        com.eldercare.backend.health.internal.catalog.domain.HealthCatalogs.VITAL_SIGN_COMPONENT,
                        created.id(),
                        false))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M06.VITAL_SIGN' AND entity_id=?",
                        Long.class,
                        String.valueOf(created.id())))
                .isEqualTo(2L);
    }

    @Test
    void FR_001_vitalSignParentAndChildrenRollBackTogether() {
        String code = "ROLLBACK_VITAL_" + TestUsers.nextId();
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            vitalSignCatalog.create(new com.eldercare.backend.health.internal.catalog.domain.VitalSignCatalogInput(
                    code,
                    code,
                    "đơn vị kiểm thử",
                    false,
                    java.util.List.of(
                            new com.eldercare.backend.health.internal.catalog.domain.VitalSignComponentDefinition(
                                    "VALUE", "Giá trị", 0))));
            status.setRollbackOnly();
        });
        assertThat(catalogs.findByCode(
                        com.eldercare.backend.health.internal.catalog.domain.HealthCatalogs.VITAL_SIGN, null, code))
                .isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog_item WHERE code=?", Long.class, code))
                .isZero();
    }

    @Autowired
    IncidentCatalogService service;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    PlatformTransactionManager transactions;

    @BeforeEach
    void manager() {
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
    }

    @Test
    void FR_039_disabledRuleCanBeReenabledAndStaleVersionWritesNothing() {
        var tx = new TransactionTemplate(transactions);
        String code = "TREND_" + TestUsers.nextId();
        var seeded = tx.execute(status -> catalogs.create(
                IncidentCatalogs.TREND_RULE,
                new NewCatalogItem<>(
                        code, code, null, 1, new TrendRuleAttributes(IncidentLevel.MEDIUM, "CFG-M05-03"))));
        var disabled = service.updateTrendRule(
                code,
                VersionEtag.of(seeded.version()),
                new TrendRuleUpdate(false, IncidentLevel.LOW, new Reason("Tắt quy tắc kiểm thử")));
        assertThat(disabled.enabled()).isFalse();
        var enabled = service.updateTrendRule(
                code,
                VersionEtag.of(disabled.version()),
                new TrendRuleUpdate(true, IncidentLevel.CRITICAL, new Reason("Bật quy tắc kiểm thử")));
        assertThat(enabled.enabled()).isTrue();
        assertThat(enabled.level()).isEqualTo(IncidentLevel.CRITICAL);
        assertThatThrownBy(() -> service.updateTrendRule(
                        code,
                        VersionEtag.of(disabled.version()),
                        new TrendRuleUpdate(false, IncidentLevel.LOW, new Reason("Phiên bản cũ"))))
                .isInstanceOf(VersionMismatchException.class);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M05.TREND_RULE' AND entity_id=?",
                        Long.class,
                        String.valueOf(seeded.id())))
                .isEqualTo(2L);
    }

    @Test
    void FR_042_catalogAndAuditRollBackTogetherWithCallerTransaction() {
        String code = "TYPE_" + TestUsers.nextId();
        var tx = new TransactionTemplate(transactions);
        tx.executeWithoutResult(status -> {
            var created = service.createIncidentType(new IncidentTypeInput(code, code, IncidentLevel.LOW, true, 1));
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM audit_log WHERE entity_type='M05.INCIDENT_TYPE' AND entity_id=?",
                            Long.class,
                            String.valueOf(created.id())))
                    .isEqualTo(1L);
            status.setRollbackOnly();
        });
        assertThat(catalogs.findByCode(IncidentCatalogs.INCIDENT_TYPE, null, code))
                .isEmpty();
    }

    @TestConfiguration
    static class Users {
        @Bean
        SwitchableUser current() {
            return new SwitchableUser();
        }
    }
}
