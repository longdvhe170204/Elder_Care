package com.eldercare.backend.incident.internal.alert.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.events.VitalSignCorrected;
import com.eldercare.backend.incident.api.events.AlertSourceCorrected;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * BR-M06-06, FR-019 (quickstart 007 Q52, lỗi B3): "nguồn đã được đính chính" gửi tới người phụ trách theo mã nhân viên;
 * {@code assigneeId} của cảnh báo là mã tài khoản nên phải đổi qua 002 trước khi dựng nhóm người nhận.
 */
class BR_71_SourceCorrectedRecipientTest {
    private static final Instant NOW = Instant.parse("2026-11-06T03:00:00Z");
    private static final long ASSIGNEE_ACCOUNT = 800L;
    private static final long ASSIGNEE_STAFF = 8800L;

    @Test
    void BR_71_sourceCorrectedNotifiesTheAssigneeByStaffId() {
        AlertStore alerts = mock(AlertStore.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        AccountApi accounts = mock(AccountApi.class);
        when(alerts.bySource(eq("CHI_SO"), anyList())).thenReturn(List.<long[]>of(new long[] {1, 40}));
        when(alerts.findById(40)).thenReturn(Optional.of(alert()));
        when(accounts.byId(ASSIGNEE_ACCOUNT))
                .thenReturn(Optional.of(new AccountView(
                        ASSIGNEE_ACCOUNT,
                        AccountType.STAFF,
                        AccountStatus.ACTIVE,
                        Set.of(Role.NURSING_STAFF),
                        ASSIGNEE_STAFF)));

        new AlertSourceCorrectionListener(alerts, events, accounts)
                .onVitalSignCorrected(new VitalSignCorrected(
                        1, 101, 5, VitalSignLevel.DANGEROUS, VitalSignLevel.NORMAL, NOW, "k", NOW));

        var published = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(published.capture());
        var event = (AlertSourceCorrected) published.getValue();
        assertThat(event.notificationRequests())
                .singleElement()
                .satisfies(request -> assertThat(request.recipientGroups())
                        .containsExactly(new RecipientGroup.Staff(ASSIGNEE_STAFF, "ALERT_SOURCE_CORRECTED")));
        verify(accounts).byId(any(Long.class));
    }

    private static AlertRecord alert() {
        return new AlertRecord(
                40,
                101L,
                7L,
                "VITAL_SIGN",
                "CHI_SO:1",
                IncidentLevel.MEDIUM,
                null,
                1,
                NOW,
                NOW,
                AlertStatus.RECEIVED,
                0,
                null,
                null,
                0,
                ASSIGNEE_ACCOUNT,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                900L,
                null,
                null,
                null,
                null,
                false,
                1);
    }
}
