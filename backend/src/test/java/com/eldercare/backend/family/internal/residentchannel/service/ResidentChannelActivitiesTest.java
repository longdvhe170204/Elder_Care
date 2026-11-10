package com.eldercare.backend.family.internal.residentchannel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.care.api.ResidentActivityReadApi;
import com.eldercare.backend.care.api.dto.ResidentActivitySessionView;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.spi.ResidentChannelBelongingPort;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.visit.service.VisitQueries;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 012 FR-083 (c), 014 FR-068a (đổi hướng duyệt 07/11): kênh người cao tuổi đọc buổi hoạt động sắp tới qua
 * {@code care.api.ResidentActivityReadApi}, không qua SPI riêng của family; thiếu bean thì mục báo "chưa có dữ liệu".
 */
class ResidentChannelActivitiesTest {

    static final long RESIDENT = 7;
    static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    final ResidentActivityReadApi activities = mock(ResidentActivityReadApi.class);

    @SuppressWarnings("unchecked")
    ResidentChannelService service(ResidentActivityReadApi api) {
        ObjectProvider<ResidentActivityReadApi> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(api);
        ObjectProvider<ResidentChannelBelongingPort> belongings = mock(ObjectProvider.class);
        ObjectProvider<ContractReadApi> contracts = mock(ObjectProvider.class);
        CurrentUser user = mock(CurrentUser.class);
        when(user.type()).thenReturn(AccountType.RESIDENT);
        when(user.residentId()).thenReturn(Optional.of(RESIDENT));
        ObjectProvider<CurrentUser> current = mock(ObjectProvider.class);
        when(current.getObject()).thenReturn(user);
        PermissionApi permissions = mock(PermissionApi.class);
        when(permissions.permissions(user))
                .thenReturn(Set.of(new PermissionKey(Function.RESIDENT_CHANNEL, Operation.VIEW)));
        BusinessTime time = mock(BusinessTime.class);
        when(time.today()).thenReturn(TODAY);
        return new ResidentChannelService(
                mock(VisitQueries.class),
                mock(RelationshipQueries.class),
                provider,
                belongings,
                contracts,
                permissions,
                time,
                current);
    }

    @Test
    void FR_083_upcomingActivities_comeFromCareApi() {
        Instant start = Instant.parse("2026-11-11T02:00:00Z");
        when(activities.upcoming(RESIDENT, TODAY, TODAY.plusDays(ResidentChannelService.UPCOMING_DAYS)))
                .thenReturn(List.of(new ResidentActivitySessionView(
                        31, "Hát cùng nhau", start, start.plusSeconds(3600), true, true)));

        var section = service(activities).upcomingActivities();

        assertThat(section.available()).isTrue();
        assertThat(section.items())
                .containsExactly(new ResidentChannelService.ChannelActivity(
                        31, "Hát cùng nhau", start, start.plusSeconds(3600), true, true));
    }

    @Test
    void FR_083_missingCareBean_marksSectionUnavailable() {
        assertThat(service(null).upcomingActivities().available()).isFalse();
    }
}
