package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.governance.service.CostAccess;
import com.eldercare.backend.billing.internal.period.persistence.CostStatementStore;
import com.eldercare.backend.billing.internal.provision.persistence.CostProvisionStore;
import com.eldercare.backend.billing.internal.provision.service.*;
import com.eldercare.backend.billing.internal.purchase.service.PurchaseAssistanceService;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.PrescriptionApi;
import com.eldercare.backend.support.FakePermissions;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class FamilyPortalCostsTest {
    final CostStatementStore statements = mock(CostStatementStore.class);
    final FinalizedCostViews views = mock(FinalizedCostViews.class);
    final CostSummaries summaries = mock(CostSummaries.class);
    final PermissionApi permissions = mock(PermissionApi.class);
    final CostAccess access = mock(CostAccess.class);
    final FieldVisibilityApi fields = mock(FieldVisibilityApi.class);
    final PrescriptionApi prescriptions = mock(PrescriptionApi.class);
    final CostProvisionStore source = mock(CostProvisionStore.class);
    final CurrentUser family = FakePermissions.user(7, Role.FAMILY_MEMBER);
    final BusinessTime time = new BusinessTime(Clock.fixed(Instant.parse("2026-11-08T01:00:00Z"), ZoneOffset.UTC));
    final FamilyPortalCosts service = new FamilyPortalCosts(
            statements,
            views,
            summaries,
            permissions,
            access,
            time,
            mock(PurchaseAssistanceService.class),
            fields,
            prescriptions,
            source);

    void allow() {
        when(access.user()).thenReturn(family);
        when(permissions.hasPermission(eq(family), any(), any()))
                .thenReturn(new PermissionResult(true, Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void FR_019_healthPermissionResolvesOnlyTheMedicationLinkedToEachCost() {
        allow();
        var row = mock(com.eldercare.backend.billing.internal.period.domain.CostStatementRecord.class);
        var linked = mock(CostItemView.class);
        var unlinked = mock(CostItemView.class);
        when(linked.id()).thenReturn(101L);
        when(linked.costType()).thenReturn(CostType.MEDICATION);
        when(linked.itemCode()).thenReturn("VT01");
        when(unlinked.id()).thenReturn(102L);
        when(unlinked.costType()).thenReturn(CostType.MEDICATION);
        when(source.medicationId(101)).thenReturn(Optional.of(501L));
        when(source.medicationId(102)).thenReturn(Optional.empty());
        var statement = new CostStatementView(
                1,
                "2026-11",
                42,
                CostStatementType.REGULAR,
                false,
                null,
                "FINALIZED",
                List.of(),
                300L,
                List.of(linked, unlinked));
        when(statements.list(null, 42L, "FINALIZED")).thenReturn(List.of(row));
        when(views.statement(row)).thenReturn(statement);
        when(fields.canView(eq(family), eq(InfoGroup.MEDICATION), any())).thenReturn(Visibility.FULL);
        var prescription = mock(com.eldercare.backend.medication.api.dto.PrescriptionView.class);
        when(prescription.medication())
                .thenReturn(new com.eldercare.backend.medication.api.dto.MedicationSummary(
                        501, "Thuốc được phép xem", null, null, null));
        when(prescriptions.history(42)).thenReturn(List.of(prescription));

        var items = service.finalizedStatements(42, family).getFirst().items();
        assertThat(items.getFirst().medicationName()).isEqualTo("Thuốc được phép xem");
        assertThat(items.getFirst().itemCode()).isEqualTo("VT01");
        assertThat(items.get(1)).isSameAs(unlinked);
        assertThat(items.get(1).medicationName()).isNull();
    }

    @Test
    void FR_038_defaultsToCurrentMonthAndRejectsCrossMonthRanges() {
        allow();
        var expected = new ProvisionalCostSummaryView("tạm tính, chưa chốt", List.of(), false);
        when(summaries.provisional(42, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 8)))
                .thenReturn(expected);
        assertThat(service.provisional(42, null, null, family)).isEqualTo(expected);
        assertThatThrownBy(() -> service.provisional(42, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 8), family))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> service.provisional(42, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 8), family))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_043_cannotSupplyAnotherAccountToReadCosts() {
        allow();
        assertThatThrownBy(() -> service.finalizedStatements(42, FakePermissions.user(8, Role.FAMILY_MEMBER)))
                .isInstanceOf(PermissionDeniedException.class);
        verifyNoInteractions(statements, prescriptions);
    }
}
