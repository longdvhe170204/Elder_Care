package com.eldercare.backend.care;

import static com.eldercare.backend.care.CongViecDuDieuKienTest.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds;
import com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.Item;
import com.eldercare.backend.care.internal.qualitycheck.persistence.QualityCheckStore;
import com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckService;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.TableRows;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 014 T023 Q41 (FR-066, Q-254, Q-262): Người phụ trách ca chỉ ghi kết quả khi ca thuộc ngoại lệ không có Trưởng tầng và
 * không ghi cho công việc của chính mình; người khác của ca không ghi được.
 */
class NguoiKiemTraTest {
    static final long ITEM = 5, LEAD_STAFF = 30, LEAD_ACCOUNT = 300;

    /** Kịch bản: ca không có Trưởng tầng?, người ghi (nhân viên), người thực hiện công việc (tài khoản), kết quả. */
    record Row(boolean noFloorManager, long staff, long performerAccount, String expected) {}

    @Test
    void FR_066_shiftLeadRecordsOnlyWithoutAFloorManagerAndNeverTheirOwnTask() {
        TableRows.each(
                List.of(
                        new Row(true, LEAD_STAFF, LEAD_ACCOUNT, "SELF_CHECK_NOT_ALLOWED"),
                        new Row(false, LEAD_STAFF, CAREGIVER_ACCOUNT, "PERMISSION_DENIED"),
                        new Row(true, 20, CAREGIVER_ACCOUNT, "PERMISSION_DENIED"),
                        new Row(true, LEAD_STAFF, CAREGIVER_ACCOUNT, "RECORDED")),
                row -> {
                    var records = mock(RecordEventApi.class);
                    var item = new Item(
                            1,
                            SHIFT,
                            FLOOR,
                            10,
                            row.performerAccount(),
                            null,
                            QualityCheckKinds.PENDING,
                            null,
                            List.of(),
                            null,
                            null,
                            null);
                    when(records.require(QualityCheckKinds.ITEMS, ITEM))
                            .thenReturn(new RecordEvent<>(ITEM, null, item, null, null, false, NOW, null, null, FLOOR));
                    var shiftPort = mock(ShiftStaffPort.class);
                    when(shiftPort.shift(SHIFT))
                            .thenReturn(Optional.of(new ShiftStaffPort.ShiftView(SHIFT, FLOOR, START, END, true)));
                    var duty = mock(DutyApi.class);
                    when(duty.shiftHasNoFloorManager(SHIFT)).thenReturn(row.noFloorManager());
                    if (!row.noFloorManager())
                        when(duty.assignedFloorManager(eq(FLOOR), any())).thenReturn(Optional.of(MANAGER_STAFF));
                    when(duty.shiftLead(eq(FLOOR), any())).thenReturn(Optional.of(LEAD_STAFF));
                    var user = mock(CurrentUser.class);
                    when(user.staffId()).thenReturn(Optional.of(row.staff()));
                    when(user.accountId()).thenReturn(row.staff() == LEAD_STAFF ? LEAD_ACCOUNT : CAREGIVER_ACCOUNT);
                    var service = new QualityCheckService(
                            records,
                            mock(SimpleRecordApi.class),
                            mock(QualityCheckStore.class),
                            provider(shiftPort),
                            provider(duty),
                            mock(AccountApi.class),
                            mock(CorrectionApi.class),
                            mock(ParameterApi.class),
                            mock(CareTaskService.class),
                            user,
                            new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)),
                            mock(AuditLogApi.class),
                            mock(ApplicationEventPublisher.class));
                    Runnable record = () ->
                            service.record(QualityCheckService.target(FLOOR), ITEM, "PASS", null, List.of(), null);
                    switch (row.expected()) {
                        case "RECORDED" -> {
                            record.run();
                            verify(records).confirm(QualityCheckKinds.ITEMS, ITEM, NOW);
                        }
                        case "PERMISSION_DENIED" ->
                            assertThatThrownBy(record::run).isInstanceOf(PermissionDeniedException.class);
                        default ->
                            assertThatThrownBy(record::run)
                                    .isInstanceOfSatisfying(
                                            BusinessException.class,
                                            e -> assertThat(e.errorCode()).isEqualTo(row.expected()));
                    }
                    verify(records, row.expected().equals("RECORDED") ? times(1) : never())
                            .updateDraft(eq(QualityCheckKinds.ITEMS), eq(ITEM), any());
                });
    }
}
