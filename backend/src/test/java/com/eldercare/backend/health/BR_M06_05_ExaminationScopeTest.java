package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.internal.examination.service.ExaminationService;
import com.eldercare.backend.health.internal.examination.service.ExaminationService.ExaminationInput;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * BR-M06-05, FR-072, FR-073 (SC-010): chẩn đoán, chỉ định, điều trị tại viện cần điều kiện chuyên môn (giấy phép bác sĩ và
 * phạm vi cơ sở, kiểm ở lớp pháp lý của 002); ghi ngoài viện và khám, đánh giá, kết quả tại viện không bị chặn; Điều dưỡng
 * chỉ ghi ngoài viện hoặc khám, kết quả tại viện với người thực hiện là Bác sĩ (CHK019).
 */
class BR_M06_05_ExaminationScopeTest {
    private static final Instant NOW = Instant.parse("2026-11-08T05:00:00Z");
    private static final long RESIDENT = 10;
    private static final long DOCTOR = 21;
    private static final long NURSE = 22;

    private final RecordEventApi records = mock(RecordEventApi.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AccountApi accounts = mock(AccountApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private ExaminationService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        ObjectProvider<CurrentUser> users = mock(ObjectProvider.class);
        when(users.getObject()).thenReturn(user);
        when(time.now()).thenReturn(NOW);
        when(accounts.hasRole(DOCTOR, Role.DOCTOR)).thenReturn(true);
        when(accounts.hasRole(NURSE, Role.DOCTOR)).thenReturn(false);
        when(records.add(any(), any(), any())).thenAnswer(call -> {
            com.eldercare.backend.administration.shared.api.dto.NewRecordEvent<?> event = call.getArgument(2);
            return new com.eldercare.backend.administration.shared.api.dto.RecordEvent<>(
                    1L,
                    call.getArgument(1),
                    event.content(),
                    null,
                    event.actorId(),
                    false,
                    event.occurredAt(),
                    null,
                    event.confirmedAt(),
                    null);
        });
        service = new ExaminationService(records, permissions, accounts, audit, users, time);
    }

    private void actAs(long id, Role role) {
        when(user.accountId()).thenReturn(id);
        when(user.roles()).thenReturn(Set.of(role));
    }

    private static ExaminationInput input(
            String type, boolean atFacility, Long performer, String facility, String who) {
        return new ExaminationInput(
                type, "Nội dung", atFacility, facility, performer, who, NOW.minusSeconds(60), List.of());
    }

    private void professional(boolean allowed) {
        when(permissions.hasPermission(
                        any(),
                        eq(new com.eldercare.backend.administration.account.api.dto.PermissionKey(
                                Function.EXAM_TREATMENT, Operation.PROFESSIONAL)),
                        any()))
                .thenReturn(
                        allowed
                                ? PermissionResult.allowed(PermissionBasis.ROLE)
                                : PermissionResult.denied(FailedLayer.LEGAL));
    }

    @Test
    void FR_073_doctorDiagnosisAtFacilityNeedsLegalConditionOthersDoNot() {
        actAs(DOCTOR, Role.DOCTOR);
        professional(false);
        for (String type : List.of("DIAGNOSIS", "ORDER", "TREATMENT")) {
            assertThatThrownBy(() -> service.record(Target.UNSPECIFIED, RESIDENT, input(type, true, null, null, null)))
                    .isInstanceOfSatisfying(
                            PermissionDeniedException.class,
                            e -> assertThat(e.errorCode()).isEqualTo("LEGAL_CONDITION_NOT_MET"));
        }
        verify(records, never()).add(any(), any(), any());

        service.record(Target.UNSPECIFIED, RESIDENT, input("EXAMINATION", true, null, null, null));
        service.record(Target.UNSPECIFIED, RESIDENT, input("ASSESSMENT", true, null, null, null));
        service.record(Target.UNSPECIFIED, RESIDENT, input("RESULT", true, null, null, null));
        service.record(Target.UNSPECIFIED, RESIDENT, input("DIAGNOSIS", false, null, "Bệnh viện X", "BS Lan"));
        verify(records, org.mockito.Mockito.times(4)).add(any(), any(), any());

        professional(true);
        service.record(Target.UNSPECIFIED, RESIDENT, input("DIAGNOSIS", true, null, null, null));
        verify(records, org.mockito.Mockito.times(5)).add(any(), any(), any());
    }

    @Test
    void FR_072_nurseRecordsOnlyExternalOrExaminationAndResultWithDoctorPerformer() {
        actAs(NURSE, Role.NURSING_STAFF);
        assertThatThrownBy(() ->
                        service.record(Target.UNSPECIFIED, RESIDENT, input("DIAGNOSIS", true, DOCTOR, null, null)))
                .isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() ->
                        service.record(Target.UNSPECIFIED, RESIDENT, input("EXAMINATION", true, NURSE, null, null)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() ->
                        service.record(Target.UNSPECIFIED, RESIDENT, input("EXAMINATION", true, null, null, null)))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() ->
                        service.record(Target.UNSPECIFIED, RESIDENT, input("RESULT", false, null, "Bệnh viện X", null)))
                .isInstanceOf(InvalidValueException.class);
        verify(records, never()).add(any(), any(), any());

        service.record(Target.UNSPECIFIED, RESIDENT, input("EXAMINATION", true, DOCTOR, null, null));
        service.record(Target.UNSPECIFIED, RESIDENT, input("RESULT", false, null, "Bệnh viện X", "BS Lan"));
        service.record(Target.UNSPECIFIED, RESIDENT, input("DIAGNOSIS", false, null, "Bệnh viện X", "BS Lan"));
        verify(records, org.mockito.Mockito.times(3)).add(any(), any(), any());
        verify(permissions, never()).hasPermission(any(), argThat(k -> k.operation() == Operation.PROFESSIONAL), any());
    }
}
