package com.eldercare.backend.incident.internal.infection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentContent;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentPeriod;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import com.eldercare.backend.support.fake.roombedallocation.FakeInfectionControlCommands;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 003 FR-024a (c) (003 module-interfaces mục 1.10a, research G-08): lệnh gỡ khoanh vùng gọi
 * {@code InfectionControlCommandApi.cordonLifted} với tầng, khu vực của vùng vừa gỡ sau khi ghi gỡ; 003 lỗi thì lệnh 007
 * lỗi theo (rollback).
 */
class FR_024a_CordonLiftedTest {
    private static final Instant AT = Instant.parse("2026-11-16T03:00:00Z");
    private static final Instant FROM = AT.minusSeconds(86_400);

    private final ContainmentStore store = mock(ContainmentStore.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final FakeInfectionControlCommands infection = new FakeInfectionControlCommands();
    private ContainmentService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void activeCordon() {
        ObjectProvider<CurrentUser> users = mock(ObjectProvider.class);
        var user = mock(CurrentUser.class);
        when(user.accountId()).thenReturn(900L);
        when(users.getObject()).thenReturn(user);
        when(time.now()).thenReturn(AT);
        var scope = new ContainmentScope(Set.of(2L, 3L), Set.of());
        var active = new ContainmentContent(new ContainmentPeriod(scope, FROM, null), "Cúm", Set.of(4L), null, null);
        when(store.lock(55L)).thenReturn(record(active, null));
        var lifted = new ContainmentContent(new ContainmentPeriod(scope, FROM, AT), "Cúm", Set.of(4L), 900L, "Hết ca");
        when(store.release(eq(55L), anyInt(), eq(AT), anyLong(), anyString())).thenReturn(record(lifted, AT));
        service = new ContainmentService(
                store,
                mock(IncidentStore.class),
                mock(LocationStructureApi.class),
                mock(AuditLogApi.class),
                mock(ApplicationEventPublisher.class),
                time,
                users,
                mock(ObjectProvider.class),
                infection);
    }

    @Test
    void FR_024a_liftingCordonTellsAllocationWithLiftedScope() {
        service.lift(55L, new Reason("Hết ca nghi nhiễm"));

        assertThat(infection.cordonsLifted)
                .containsExactly(new FakeInfectionControlCommands.CordonLifted(Set.of(2L, 3L), Set.of(), AT));
    }

    @Test
    void FR_024a_allocationFailureFailsTheLiftCommand() {
        infection.fail(new IllegalStateException("003 lỗi"));

        assertThatThrownBy(() -> service.lift(55L, new Reason("Hết ca nghi nhiễm")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("003 lỗi");
        verify(store).release(eq(55L), anyInt(), eq(AT), anyLong(), anyString());
        assertThat(infection.cordonsLifted).isEmpty();
    }

    private static SimpleRecord<ContainmentContent> record(ContainmentContent content, Instant lockedAt) {
        return new SimpleRecord<>(
                55,
                null,
                null,
                lockedAt == null ? "ACTIVE" : "LIFTED",
                content,
                null,
                null,
                null,
                lockedAt,
                FROM,
                900,
                1);
    }
}
