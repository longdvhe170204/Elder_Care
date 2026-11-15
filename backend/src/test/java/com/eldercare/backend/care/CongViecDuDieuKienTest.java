package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.care.internal.qualitycheck.domain.CompletedTask;
import com.eldercare.backend.care.internal.qualitycheck.persistence.QualityCheckStore;
import com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckService;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.TableRows;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 014 T023 Q39 (FR-060, SC-014): công việc của chính Trưởng tầng, công việc làm lại, kết quả đã Hủy ghi nhận và thứ không
 * phải công việc chăm sóc của ca (liều thuốc của 006 không có trong {@code care_task}) không được chọn, không được đếm.
 */
class CongViecDuDieuKienTest {
    static final Instant NOW = Instant.parse("2026-11-08T03:00:00Z");
    static final Instant START = Instant.parse("2026-11-08T00:00:00Z");
    static final Instant END = Instant.parse("2026-11-08T08:00:00Z");
    static final long SHIFT = 1, FLOOR = 2, MANAGER_STAFF = 10, MANAGER_ACCOUNT = 100, CAREGIVER_ACCOUNT = 200;

    @Test
    void FR_060_ownReworkVoidedAndNonCareTasksAreNeitherSelectableNorCounted() {
        var shiftPort = mock(ShiftStaffPort.class);
        var shift = new ShiftStaffPort.ShiftView(SHIFT, FLOOR, START, END, true);
        when(shiftPort.shift(SHIFT)).thenReturn(Optional.of(shift));
        var duty = mock(DutyApi.class);
        when(duty.assignedFloorManager(eq(FLOOR), any())).thenReturn(Optional.of(MANAGER_STAFF));
        var accounts = mock(AccountApi.class);
        when(accounts.byId(MANAGER_ACCOUNT))
                .thenReturn(Optional.of(new AccountView(
                        MANAGER_ACCOUNT,
                        AccountType.STAFF,
                        AccountStatus.ACTIVE,
                        Set.of(Role.FLOOR_MANAGER),
                        MANAGER_STAFF)));
        when(accounts.byId(CAREGIVER_ACCOUNT))
                .thenReturn(Optional.of(new AccountView(
                        CAREGIVER_ACCOUNT, AccountType.STAFF, AccountStatus.ACTIVE, Set.of(Role.CAREGIVER), 20L)));
        var store = mock(QualityCheckStore.class);
        when(store.completed(SHIFT, FLOOR, START, END))
                .thenReturn(List.of(
                        task(10, CAREGIVER_ACCOUNT, "AD_HOC"),
                        task(11, MANAGER_ACCOUNT, "AD_HOC"),
                        task(12, CAREGIVER_ACCOUNT, "REWORK"),
                        task(13, CAREGIVER_ACCOUNT, "AD_HOC")));
        var corrections = mock(CorrectionApi.class);
        when(corrections.current(eq("M04.CARE_RESULT"), anyCollection()))
                .thenReturn(Map.of(13L, new CurrentValue(null, true, true)));
        var user = mock(CurrentUser.class);
        when(user.staffId()).thenReturn(Optional.of(MANAGER_STAFF));
        when(user.accountId()).thenReturn(MANAGER_ACCOUNT);
        var service = new QualityCheckService(
                mock(RecordEventApi.class),
                mock(SimpleRecordApi.class),
                store,
                provider(shiftPort),
                provider(duty),
                accounts,
                corrections,
                mock(ParameterApi.class),
                mock(CareTaskService.class),
                user,
                new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)),
                mock(AuditLogApi.class),
                mock(ApplicationEventPublisher.class));

        assertThat(service.status(shift, NOW).eligible()).isEqualTo(1);
        // 999: mã của một liều thuốc (006), không phải công việc của ca.
        TableRows.each(
                List.of(11L, 12L, 13L, 999L),
                id -> assertThatThrownBy(
                                () -> service.select(QualityCheckService.target(FLOOR), SHIFT, FLOOR, List.of(id)))
                        .isInstanceOfSatisfying(
                                BusinessException.class,
                                e -> assertThat(e.errorCode()).isEqualTo("TASK_NOT_ELIGIBLE")));
    }

    static CompletedTask task(long id, long recordedBy, String basis) {
        return new CompletedTask(id, null, recordedBy, NOW.minusSeconds(600), basis, SHIFT, 5, 7L, null);
    }

    @SuppressWarnings("unchecked")
    static <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
