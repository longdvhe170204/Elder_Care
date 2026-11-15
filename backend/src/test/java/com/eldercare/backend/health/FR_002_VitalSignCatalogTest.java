package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.health.internal.catalog.domain.*;
import com.eldercare.backend.health.internal.catalog.service.VitalSignCatalogService;
import com.eldercare.backend.support.fake.shared.FakeCatalogApi;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

class FR_002_VitalSignCatalogTest {
    private final FakeCatalogApi catalogs = new FakeCatalogApi();
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final ParameterApi parameters = mock(ParameterApi.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<CurrentUser> users = mock(ObjectProvider.class);

    private final CurrentUser user = mock(CurrentUser.class);
    private final VitalSignCatalogService service = new VitalSignCatalogService(catalogs, parameters, audit, users);

    @BeforeEach
    void setup() {
        when(users.getObject()).thenReturn(user);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        when(parameters.read(ParameterCode.CFG_M06_03))
                .thenReturn(new ParameterValue(
                        ParameterCode.CFG_M06_03,
                        ParameterKind.TABLE,
                        JsonMapper.builder().build().readTree("{}")));
    }

    @Test
    void FR_002_updatesMetadataWithEtagAndDoesNotRewriteComponents() {
        var created = service.create(new VitalSignCatalogInput(
                "TEST", "Tên cũ", "đơn vị", false, List.of(new VitalSignComponentDefinition("VALUE", "Giá trị", 1))));
        var changed = service.update(
                created.id(),
                "\"0\"",
                new VitalSignCatalogInput("TEST", "Tên mới", "đơn vị mới", true, created.components()));
        assertThat(changed.name()).isEqualTo("Tên mới");
        assertThat(changed.vitalSign()).isTrue();
        assertThat(changed.components()).isEqualTo(created.components());
        assertThatThrownBy(() -> service.update(
                        created.id(),
                        "\"0\"",
                        new VitalSignCatalogInput("TEST", "Ghi đè", "đơn vị", false, created.components())))
                .isInstanceOf(VersionMismatchException.class);
    }

    @Test
    void FR_002_deactivatesWithoutDeletingReferencedCatalogOrItsComponents() {
        var created = service.create(new VitalSignCatalogInput(
                "TEST",
                "Chỉ số thử",
                "đơn vị",
                false,
                List.of(new VitalSignComponentDefinition("VALUE", "Giá trị", 1))));
        var stopped = service.deactivate(created.id(), new Reason("Không dùng cho lần đo mới"));
        assertThat(stopped.active()).isFalse();
        assertThat(catalogs.require(HealthCatalogs.VITAL_SIGN, created.id())).isNotNull();
        assertThat(catalogs.list(HealthCatalogs.VITAL_SIGN_COMPONENT, created.id(), false))
                .hasSize(1);
    }

    @Test
    void FR_001_listReadsCurrentPhysicalRangesOnceFromParameterApi() {
        service.create(new VitalSignCatalogInput(
                "TEST",
                "Chỉ số thử",
                "đơn vị",
                false,
                List.of(new VitalSignComponentDefinition("VALUE", "Giá trị", 1))));
        when(parameters.read(ParameterCode.CFG_M06_03))
                .thenReturn(new ParameterValue(
                        ParameterCode.CFG_M06_03,
                        ParameterKind.TABLE,
                        JsonMapper.builder().build().readTree("{\"TEST\":{\"VALUE\":{\"min\":1,\"max\":5}}}")));
        clearInvocations(parameters);
        assertThat(service.list()).singleElement().satisfies(item -> {
            assertThat(item.missingValidRange()).isFalse();
            assertThat(item.validRange().get("VALUE").get("max").intValue()).isEqualTo(5);
        });
        verify(parameters, times(1)).read(ParameterCode.CFG_M06_03);
    }
}
