package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.api.events.HandoverCorrected;
import com.cambae.backend.m09.handover.internal.domain.HandoverRecord;
import com.cambae.backend.m09.handover.internal.persistence.HandoverStore;
import com.cambae.backend.m09.handover.internal.service.*;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import com.cambae.backend.m15.account.api.spi.DutyApi;
import com.cambae.backend.m15.correction.api.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/** FR-057: người xác nhận lấy từ liên kết tài khoản 002, kể cả cột account_id của M09 chưa nối. */
class HandoverCorrectionNotificationsTest {
    @Test
    void FR_057_correctionNotifiesTheRecordedConfirmerUsingTheAccountLink() {
        var at = Instant.parse("2026-10-27T01:00:00Z");
        var store = mock(HandoverStore.class);
        var contents = mock(HandoverContents.class);
        var shifts = mock(ShiftStore.class);
        var jdbc = mock(JdbcTemplate.class);
        var accounts = mock(AccountApi.class);
        var publisher = mock(ApplicationEventPublisher.class);
        var time = mock(BusinessTime.class);
        var user = mock(CurrentUser.class);
        var beans = new StaticListableBeanFactory(Map.of("user", user, "handovers", mock(HandoverService.class)));
        var correction = new HandoverCorrection(
                store,
                contents,
                shifts,
                accounts,
                mock(DutyApi.class),
                jdbc,
                publisher,
                new HandoverNotifications(),
                time,
                beans.getBeanProvider(HandoverService.class),
                beans.getBeanProvider(CurrentUser.class),
                mock(FieldVisibilityApi.class));
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(time.now()).thenReturn(at);
        when(store.record(30, false)).thenReturn(new HandoverRecord(30, 20, 21L, 10, 100L, "CONFIRMED", at, at, at, 0));
        when(shifts.shift(20, false))
                .thenReturn(new WorkShift(
                        20, 1, 3, at.minusSeconds(28800), at, "FLOOR", 5L, true, "CLOSED", 10L, false, 0));
        when(contents.original(30)).thenReturn(Map.of("generalAssessment", "Nhận định gốc"));
        when(jdbc.queryForObject("SELECT confirmed_by FROM shift_handover WHERE id=?", Long.class, 30L))
                .thenReturn(200L);
        when(accounts.byId(200))
                .thenReturn(Optional.of(new AccountView(
                        200, AccountType.STAFF, AccountStatus.ACTIVE, Set.of(Role.NURSING_STAFF), 11L)));

        correction.afterCorrection(new CorrectionView(
                40,
                "M09.HANDOVER",
                30,
                1,
                CorrectionType.AMEND_CONTENT,
                Map.of("generalAssessment", "Nhận định đã đính chính"),
                "Sửa nhận định",
                100,
                CreationMethod.A,
                null,
                at));

        var event = ArgumentCaptor.forClass(HandoverCorrected.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().notificationRequests().getFirst().recipientGroups())
                .contains(new RecipientGroup.Staff(11, "HANDOVER_CORRECTED"));
    }
}
