package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationResponse;
import com.eldercare.backend.medication.internal.reconciliation.service.MedicationReconciliationService;
import com.eldercare.backend.medication.internal.reconciliation.service.ReconciliationWorklist;
import com.eldercare.backend.medication.web.MedicationReconciliationController;
import com.eldercare.backend.medication.web.request.ReasonRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/**
 * Q-447 (test luồng BF-01 2026-11-26): lệnh Hủy phiếu đối chiếu chỉ cho người có quyền ghi phiếu đối chiếu (Phụ lục 27
 * dòng "Đối chiếu thuốc, thuốc gia đình gửi": Bác sĩ, Điều dưỡng). Các lệnh ghi khác của phiếu kiểm quyền cùng cách: CREATE
 * lập phiếu; UPDATE lưu dòng, gửi, đề nghị đính chính; CONFIRM trả lại, xác nhận phiếu và đính chính.
 */
class ReconciliationCancelPermissionTest {

    private final MedicationReconciliationService service = mock(MedicationReconciliationService.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final MedicationReconciliationController controller =
            new MedicationReconciliationController(service, user, mock(ReconciliationWorklist.class), permissions);

    @Test
    void Q_447_withoutReconciliationUpdate_cancelIsForbidden() {
        when(permissions.permissions(user))
                .thenReturn(Set.of(new PermissionKey(Function.MEDICATION_RECONCILIATION, Operation.VIEW)));

        PermissionDeniedException denied = catchThrowableOfType(
                PermissionDeniedException.class,
                () -> controller.cancel(5L, null, new ReasonRequest("Lập nhầm lý do")));

        assertThat(denied).isNotNull();
        assertThat(denied.errorCode()).isEqualTo("PERMISSION_DENIED");
        verifyNoInteractions(service);
    }

    private Map<String, ThrowingCallable> writeCommands() {
        ReasonRequest reason = new ReasonRequest("Thiếu dòng");
        return Map.of(
                "create", () -> controller.create(7L, null, null),
                "updateItems", () -> controller.updateItems(5L, "\"1\"", null, List.of()),
                "submit", () -> controller.submit(5L, null),
                "return", () -> controller.returnTicket(5L, null, reason),
                "confirm", () -> controller.confirm(5L, null),
                "proposeCorrection", () -> controller.proposeCorrection(5L, null, null),
                "confirmCorrection", () -> controller.confirmCorrection(9L, null),
                "rejectCorrection", () -> controller.rejectCorrection(9L, null, reason));
    }

    @Test
    void appendix27_viewOnly_everyWriteCommandIsForbidden() {
        when(permissions.permissions(user))
                .thenReturn(Set.of(new PermissionKey(Function.MEDICATION_RECONCILIATION, Operation.VIEW)));

        writeCommands()
                .forEach((name, command) -> assertThat(catchThrowableOfType(PermissionDeniedException.class, command))
                        .as(name)
                        .isNotNull());
        verifyNoInteractions(service);
    }

    @Test
    void appendix27_withoutConfirm_returnAndConfirmCommandsAreForbidden() {
        when(permissions.permissions(user))
                .thenReturn(Set.of(
                        new PermissionKey(Function.MEDICATION_RECONCILIATION, Operation.CREATE),
                        new PermissionKey(Function.MEDICATION_RECONCILIATION, Operation.UPDATE)));
        Map<String, ThrowingCallable> commands = writeCommands();

        for (String name : List.of("return", "confirm", "confirmCorrection", "rejectCorrection")) {
            assertThat(catchThrowableOfType(PermissionDeniedException.class, commands.get(name)))
                    .as(name)
                    .isNotNull();
        }
        verifyNoInteractions(service);
    }

    @Test
    void Q_447_withReconciliationUpdate_cancelReachesTheService() {
        when(permissions.permissions(user))
                .thenReturn(Set.of(new PermissionKey(Function.MEDICATION_RECONCILIATION, Operation.UPDATE)));
        ReconciliationResponse cancelled = new ReconciliationResponse();
        cancelled.setVersion(3);
        when(service.cancel(anyLong(), any())).thenReturn(cancelled);

        controller.cancel(5L, null, new ReasonRequest("Lập nhầm lý do"));

        verify(service).cancel(5L, "Lập nhầm lý do");
    }
}
