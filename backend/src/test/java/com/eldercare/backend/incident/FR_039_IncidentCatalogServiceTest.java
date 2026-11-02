package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.incident.internal.alert.domain.IncidentCatalogs;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeAttributes;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeInput;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeAttributes;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeInput;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleAttributes;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleUpdate;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.support.fake.shared.FakeCatalogApi;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class FR_039_IncidentCatalogServiceTest {
    @Test
    void FR_042_specialTypesCannotBeDeactivatedButCustomTypesCan() {
        for (String code :
                java.util.List.of("FALL", "SUSPECTED_INFECTION", "FACILITY", "BELONGINGS_LOST", "BELONGINGS_DAMAGED")) {
            var item = catalogs.create(
                    IncidentCatalogs.INCIDENT_TYPE,
                    new NewCatalogItem<>(
                            code, code, null, 0, new IncidentTypeAttributes(IncidentLevel.MEDIUM, false, false)));
            assertThatThrownBy(() -> service.updateIncidentType(
                            item.id(),
                            "\"0\"",
                            new IncidentTypeInput(code, code, IncidentLevel.MEDIUM, false, 0),
                            false))
                    .isInstanceOf(BusinessException.class);
        }
        var custom =
                service.createIncidentType(new IncidentTypeInput("CUSTOM", "Loại riêng", IncidentLevel.LOW, false, 0));
        var stopped = service.updateIncidentType(
                custom.id(),
                "\"0\"",
                new IncidentTypeInput("CUSTOM", "Loại riêng", IncidentLevel.LOW, false, 0),
                false);
        assertThat(stopped.active()).isFalse();
        assertThat(service.incidentTypes()).extracting(v -> v.code()).contains("CUSTOM", "FALL");
    }

    @Test
    void FR_039_catalogListsExposeSeedAttributesWithoutWritingAudit() {
        catalogs.create(
                IncidentCatalogs.TREND_RULE,
                new NewCatalogItem<>(
                        "WEIGHT_LOSS",
                        "Sụt cân",
                        null,
                        0,
                        new TrendRuleAttributes(IncidentLevel.MEDIUM, "CFG-M05-03")));
        catalogs.create(
                IncidentCatalogs.PROGRESSION_TYPE,
                new NewCatalogItem<>(
                        "CHEST_COMPRESSION",
                        "Ép tim",
                        null,
                        0,
                        new ProgressionTypeAttributes(ProgressionGroup.ACTION, true)));
        assertThat(service.trendRules())
                .singleElement()
                .satisfies(v -> assertThat(v.enabled()).isTrue());
        assertThat(service.progressionTypes())
                .singleElement()
                .satisfies(v -> assertThat(v.resuscitation()).isTrue());
        verifyNoInteractions(audit);
    }

    private final FakeCatalogApi catalogs = new FakeCatalogApi();
    private final AuditLogApi audit = mock(AuditLogApi.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<CurrentUser> users = mock(ObjectProvider.class);

    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentCatalogService service = new IncidentCatalogService(catalogs, audit, users);

    @BeforeEach
    void manager() {
        when(users.getObject()).thenReturn(user);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
    }

    @Test
    void FR_039_staleEtagDoesNotChangeTheRuleOrWriteAudit() {
        var seeded = catalogs.create(
                IncidentCatalogs.TREND_RULE,
                new NewCatalogItem<>(
                        "WEIGHT_LOSS",
                        "Sụt cân",
                        null,
                        1,
                        new TrendRuleAttributes(IncidentLevel.MEDIUM, "CFG-M05-03")));
        assertThatThrownBy(() -> service.updateTrendRule(
                        "WEIGHT_LOSS", "\"99\"", new TrendRuleUpdate(false, IncidentLevel.LOW, new Reason("Tắt"))))
                .isInstanceOf(VersionMismatchException.class);
        assertThat(catalogs.require(IncidentCatalogs.TREND_RULE, seeded.id())).isEqualTo(seeded);
        verifyNoInteractions(audit);
    }

    @Test
    void FR_039_missingReasonDoesNotChangeTheRule() {
        assertThatThrownBy(() -> service.updateTrendRule(
                        "WEIGHT_LOSS", "\"0\"", new TrendRuleUpdate(false, IncidentLevel.LOW, null)))
                .isInstanceOf(ReasonRequiredException.class);
        verifyNoInteractions(audit);
    }

    @Test
    void FR_042_onlyManagerMayConfigureIncidentCatalogs() {
        when(user.roles()).thenReturn(Set.of(Role.OFFICE_STAFF));
        assertThatThrownBy(() ->
                        service.createIncidentType(new IncidentTypeInput("OTHER", "Khác", IncidentLevel.LOW, false, 1)))
                .isInstanceOf(PermissionDeniedException.class);
        verifyNoInteractions(audit);
    }

    @Test
    void FR_042_changingCodeCannotBypassRulesAttachedToTheOriginalType() {
        var seeded = catalogs.create(
                IncidentCatalogs.INCIDENT_TYPE,
                new NewCatalogItem<>(
                        "FALL", "Ngã", null, 1, new IncidentTypeAttributes(IncidentLevel.CRITICAL, false, false)));
        assertThatThrownBy(() -> service.updateIncidentType(
                        seeded.id(), "\"0\"", new IncidentTypeInput("OTHER", "Khác", IncidentLevel.LOW, false, 1)))
                .isInstanceOf(BusinessException.class);
        assertThat(catalogs.require(IncidentCatalogs.INCIDENT_TYPE, seeded.id()))
                .isEqualTo(seeded);
        verifyNoInteractions(audit);
    }

    @Test
    void FR_039_updatesLevelAndEnabledStateWithOneReasonedAuditEntry() {
        var seeded = catalogs.create(
                IncidentCatalogs.TREND_RULE,
                new NewCatalogItem<>(
                        "WEIGHT_LOSS",
                        "Sụt cân",
                        null,
                        1,
                        new TrendRuleAttributes(IncidentLevel.MEDIUM, "CFG-M05-03")));

        var result = service.updateTrendRule(
                "WEIGHT_LOSS",
                VersionEtag.of(seeded.version()),
                new TrendRuleUpdate(false, IncidentLevel.CRITICAL, new Reason("Tăng mức theo quyết định viện")));

        assertThat(result.code()).isEqualTo("WEIGHT_LOSS");
        assertThat(result.enabled()).isFalse();
        assertThat(result.level()).isEqualTo(IncidentLevel.CRITICAL);
        assertThat(result.parameterCode()).isEqualTo("CFG-M05-03");

        var entry = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(audit).write(entry.capture());
        assertThat(entry.getValue().entityType()).isEqualTo("M05.TREND_RULE");
        assertThat(entry.getValue().reason()).isEqualTo("Tăng mức theo quyết định viện");
        assertThat(entry.getValue().before()).containsEntry("enabled", true).containsEntry("level", "MEDIUM");
        assertThat(entry.getValue().after()).containsEntry("enabled", false).containsEntry("level", "CRITICAL");
    }

    @Test
    void FR_042_updateKeepsAllowsNoResidentReadOnlyAndAuditsNonHealthChange() {
        var seeded = catalogs.create(
                IncidentCatalogs.INCIDENT_TYPE,
                new NewCatalogItem<>(
                        "FACILITY",
                        "Cơ sở vật chất",
                        null,
                        1,
                        new IncidentTypeAttributes(IncidentLevel.LOW, true, true)));

        var result = service.updateIncidentType(
                seeded.id(),
                VersionEtag.of(seeded.version()),
                new IncidentTypeInput("FACILITY", "Cơ sở vật chất và thiết bị", IncidentLevel.MEDIUM, false, 2));

        assertThat(result.allowsNoResident()).isTrue();
        assertThat(result.nonHealth()).isFalse();
        assertThat(result.name()).isEqualTo("Cơ sở vật chất và thiết bị");

        var entry = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(audit).write(entry.capture());
        assertThat(entry.getValue().entityType()).isEqualTo("M05.INCIDENT_TYPE");
        assertThat(entry.getValue().before()).containsEntry("nonHealth", true);
        assertThat(entry.getValue().after()).containsEntry("nonHealth", false);
    }

    @Test
    void FR_042_createCustomIncidentTypeNeverGrantsResidentlessRecording() {
        var created = service.createIncidentType(
                new IncidentTypeInput("OTHER_CLINICAL", "Sự cố lâm sàng khác", IncidentLevel.MEDIUM, false, 20));

        assertThat(created.allowsNoResident()).isFalse();
        assertThat(catalogs.require(IncidentCatalogs.INCIDENT_TYPE, created.id())
                        .attributes())
                .isEqualTo(new IncidentTypeAttributes(IncidentLevel.MEDIUM, false, false));
    }

    @Test
    void BR_M05_20_createdProgressionKeepsItsResuscitationFlag() {
        var created = service.createProgressionType(
                new ProgressionTypeInput("AIRWAY_SUPPORT", "Hỗ trợ đường thở", ProgressionGroup.ACTION, true, 20));

        assertThat(created.resuscitation()).isTrue();
        assertThat(catalogs.require(IncidentCatalogs.PROGRESSION_TYPE, created.id())
                        .attributes())
                .isEqualTo(new ProgressionTypeAttributes(ProgressionGroup.ACTION, true));
    }
}
