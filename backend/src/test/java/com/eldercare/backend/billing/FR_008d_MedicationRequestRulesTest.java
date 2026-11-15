package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.api.spi.ProcurementIntakePort;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementCommandInput;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementContent;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow.Line;
import com.eldercare.backend.billing.internal.procurement.persistence.ProcurementRequestStore;
import com.eldercare.backend.billing.internal.procurement.service.ProcurementNotices;
import com.eldercare.backend.billing.internal.procurement.service.ProcurementRequestService;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.PrescriptionApi;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import com.eldercare.backend.support.TableRows;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 019 T028 (FR-008d, BR-M11-17; ngân sách test AGENTS.md mục 4): nhánh từ chối phụ của yêu cầu loại thuốc, kiểm trên dịch
 * vụ với nguồn giả: thiếu mức ưu tiên, thời điểm cần, người cao tuổi hoặc đơn của dòng → 422 trước mọi kiểm quyền;
 * Điều dưỡng (không là Phụ trách bộ phận) lập loại khác thuốc, ghi đã mua → 403 {@code NOT_DEPARTMENT_LEAD}.
 */
class FR_008d_MedicationRequestRulesTest {
    static final long DEPARTMENT = 7, NURSE = 11, LEAD = 12;

    final ProcurementRequestStore store = mock(ProcurementRequestStore.class);
    final DutyApi duty = mock(DutyApi.class);
    final CurrentUser user = mock(CurrentUser.class);
    final PermissionApi permissions = mock(PermissionApi.class);
    final PrescriptionApi prescriptions = mock(PrescriptionApi.class);
    final ProcurementIntakePort intakes = mock(ProcurementIntakePort.class);
    final ObjectProvider<DutyApi> dutyProvider = provider(duty);
    final ObjectProvider<PrescriptionApi> prescriptionProvider = provider(prescriptions);
    final ObjectProvider<ProcurementIntakePort> intakeProvider = provider(intakes);
    final ProcurementRequestService service = new ProcurementRequestService(
            store,
            dutyProvider,
            user,
            mock(BusinessTime.class),
            mock(ParameterApi.class),
            mock(FileApi.class),
            mock(AuditLogApi.class),
            mock(ApplicationEventPublisher.class),
            mock(ProcurementNotices.class),
            permissions,
            mock(ScopeApi.class),
            mock(FieldVisibilityApi.class),
            prescriptionProvider,
            intakeProvider);

    @SuppressWarnings("unchecked")
    static <T> ObjectProvider<T> provider(T bean) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(bean);
        when(provider.getIfAvailable()).thenReturn(bean);
        return provider;
    }

    static Line line(Long resident, Long prescription) {
        return new Line(
                "Amlodipin", BigDecimal.TEN, 2000, 0, resident, prescription, "Amlodipin", "5 mg", "Viên nén", "viên");
    }

    static ProcurementContent content(ProcurementType type, String priority, Instant neededBy, Line line) {
        return new ProcurementContent(
                DEPARTMENT, type, priority, neededBy, List.of(), List.of(line), 0, "Sắp hết", null, null, null);
    }

    void asNurse() {
        when(user.staffId()).thenReturn(Optional.of(NURSE));
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(duty.departmentOf(anyLong(), any())).thenReturn(Optional.of(DEPARTMENT));
        when(duty.departmentLead(DEPARTMENT)).thenReturn(Optional.of(LEAD));
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
    }

    @Test
    void FR_008d_missingPriorityNeededByResidentOrPrescriptionIs422BeforeAnyCheck() {
        asNurse();
        var at = Instant.parse("2026-11-12T01:00:00Z");
        TableRows.each(
                List.of(
                        content(ProcurementType.MEDICATION, null, at, line(1L, 2L)),
                        content(ProcurementType.MEDICATION, "URGENT", null, line(1L, 2L)),
                        content(ProcurementType.MEDICATION, "URGENT", at, line(null, 2L)),
                        content(ProcurementType.MEDICATION, "URGENT", at, line(1L, null))),
                draft -> assertThatThrownBy(() -> service.create(draft)).isInstanceOf(InvalidValueException.class));
        verifyNoInteractions(store, prescriptions, permissions);
    }

    @Test
    void FR_008d_nurseCannotCreateOtherTypesNorRecordPurchased() {
        asNurse();
        assertThatThrownBy(() -> service.create(content(ProcurementType.INGREDIENT, null, null, line(null, null))))
                .isInstanceOfSatisfying(
                        PermissionDeniedException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.errorCode())
                                .isEqualTo("NOT_DEPARTMENT_LEAD"));
        var row = mock(ProcurementRequestRow.class);
        when(row.status()).thenReturn(ProcurementStatus.APPROVED);
        when(row.departmentId()).thenReturn(DEPARTMENT);
        when(store.lock(5)).thenReturn(Optional.of(row));
        assertThatThrownBy(() -> service.recordPurchased(Target.UNSPECIFIED, 5, ProcurementCommandInput.EMPTY))
                .isInstanceOfSatisfying(
                        PermissionDeniedException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.errorCode())
                                .isEqualTo("NOT_DEPARTMENT_LEAD"));
    }
}
