package com.eldercare.backend.family.internal.portal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.care.api.DailyCareScheduleApi;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.residentchannel.service.ResidentChannelService;
import java.time.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/** 012 FR-076, FR-083 (a): ai xem được Lịch chăm sóc hằng ngày và khi nào hiện giá trị đo. */
class FR_076_DailyCareScheduleViewTest {
    private static final long FAMILY = 3, RESIDENT = 9;
    private static final LocalDate TODAY = LocalDate.of(2035, 5, 2);

    private final RelationshipQueries relationships = mock(RelationshipQueries.class);
    private final FamilyRightsApi rights = mock(FamilyRightsApi.class);
    private final ResidentChannelService channel = mock(ResidentChannelService.class);
    private final DailyCareScheduleApi schedule = mock(DailyCareScheduleApi.class);
    private final CurrentUser user = mock(CurrentUser.class);

    @SuppressWarnings("unchecked")
    private DailyCareScheduleView view() {
        ObjectProvider<DailyCareScheduleApi> api = mock(ObjectProvider.class);
        when(api.getIfAvailable()).thenReturn(schedule);
        ObjectProvider<CurrentUser> users = mock(ObjectProvider.class);
        when(users.getObject()).thenReturn(user);
        when(schedule.onDate(anyLong(), any(), anyBoolean())).thenReturn(List.of());
        var now = TODAY.atTime(9, 0).atZone(BusinessTime.ZONE).toInstant();
        return new DailyCareScheduleView(
                relationships, rights, channel, api, users, new BusinessTime(Clock.fixed(now, ZoneOffset.UTC)));
    }

    private void family(boolean related, boolean healthRight, boolean consent) {
        when(user.type()).thenReturn(AccountType.FAMILY_MEMBER);
        when(user.familyMemberId()).thenReturn(Optional.of(FAMILY));
        when(relationships.active(FAMILY, RESIDENT))
                .thenReturn(related ? Optional.of(mock(FamilyRelationship.class)) : Optional.empty());
        when(relationships.rightEnabledAt(eq(FAMILY), eq(RESIDENT), eq(FamilyRight.VIEW_HEALTH), any()))
                .thenReturn(healthRight);
        when(rights.rightEffective(eq(FAMILY), eq(RESIDENT), eq(FamilyRight.VIEW_HEALTH), any()))
                .thenReturn(consent);
    }

    @Test
    void FR_076_familyWithEffectiveHealthRightSeesMeasurements() {
        family(true, true, true);
        var result = view().forFamily(RESIDENT, null);
        assertThat(result.measurementsShown()).isTrue();
        assertThat(result.date()).isEqualTo(TODAY);
        verify(schedule).onDate(RESIDENT, TODAY, true);
    }

    @Test
    void FR_076_rightWithoutConsentHidesMeasurements() {
        family(true, true, false);
        assertThat(view().forFamily(RESIDENT, TODAY.minusMonths(1)).measurementsShown())
                .isFalse();
        verify(schedule).onDate(RESIDENT, TODAY.minusMonths(1), false);
    }

    @Test
    void FR_076_noActiveRelationshipIsDenied() {
        family(false, true, true);
        assertThatThrownBy(() -> view().forFamily(RESIDENT, null)).isInstanceOf(PermissionDeniedException.class);
        verifyNoInteractions(schedule);
    }

    @Test
    void FR_083_residentSeesOwnScheduleWithoutMeasurements() {
        when(channel.self()).thenReturn(RESIDENT);
        assertThat(view().forResident(null).measurementsShown()).isFalse();
        verify(schedule).onDate(RESIDENT, TODAY, false);
    }
}
