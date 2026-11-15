package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.incident.internal.alert.domain.IncidentCatalogs;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeInput;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeAttributes;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleAttributes;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleUpdate;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.support.fake.shared.FakeCatalogApi;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class FR_039_IncidentCatalogServiceTest {

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
                        "WEIGHT_LOSS", "\"99\"", new TrendRuleUpdate(false, IncidentLevel.LOW), new Reason("Tắt")))
                .isInstanceOf(VersionMismatchException.class);
        assertThat(catalogs.require(IncidentCatalogs.TREND_RULE, seeded.id())).isEqualTo(seeded);
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
}
