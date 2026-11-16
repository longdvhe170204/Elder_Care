package com.eldercare.backend.family.internal.portal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.visit.service.VisitQueries;
import com.eldercare.backend.nutrition.api.DietApi;
import com.eldercare.backend.nutrition.api.GeneralMenuApi;
import com.eldercare.backend.nutrition.api.dto.CurrentDietView;
import com.eldercare.backend.nutrition.api.dto.DishByMealView;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 012 FR-046 (1), (2): cổng người thân lấy thực đơn chung của viện (loại "chung") và chế độ ăn riêng (loại "sức khỏe")
 * từ 011 ({@code GeneralMenuApi}, {@code DietApi}); 011 chưa có thì mục đó ghi chưa có dữ liệu.
 */
class FR_046_PortalMenuDietTest {

    private static final long RESIDENT = 101;
    private static final long FAMILY_MEMBER = 5;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 17);
    private static final Instant NOW = Instant.parse("2026-11-17T03:00:00Z");

    private final RelationshipQueries relationships = mock(RelationshipQueries.class);
    private final FamilyRightsApi rights = mock(FamilyRightsApi.class);
    private final ResidentApi residents = mock(ResidentApi.class, RETURNS_DEEP_STUBS);
    private final BusinessTime time = mock(BusinessTime.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<GeneralMenuApi> menus = mock(ObjectProvider.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<DietApi> diets = mock(ObjectProvider.class);

    @SuppressWarnings("unchecked")
    private final ObjectProvider<CurrentUser> currentUser = mock(ObjectProvider.class);

    private PortalService portal;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void familyMemberOfTheResident() {
        CurrentUser user = mock(CurrentUser.class);
        when(user.type()).thenReturn(AccountType.FAMILY_MEMBER);
        when(user.familyMemberId()).thenReturn(Optional.of(FAMILY_MEMBER));
        when(currentUser.getObject()).thenReturn(user);
        when(relationships.active(FAMILY_MEMBER, RESIDENT)).thenReturn(Optional.of(mock(FamilyRelationship.class)));
        when(residents.status(RESIDENT)).thenReturn(ResidentStatus.IN_RESIDENCE);
        when(time.now()).thenReturn(NOW);
        when(time.today()).thenReturn(TODAY);
        when(time.startOfDay(any())).thenReturn(NOW);
        ResidentLocationApi locations = mock(ResidentLocationApi.class);
        when(locations.current(anyLong())).thenReturn(Optional.empty());
        portal = new PortalService(
                relationships,
                rights,
                mock(VisitQueries.class),
                residents,
                mock(ResidentStatusApi.class),
                locations,
                mock(HealthItemApi.class),
                mock(AllergenApi.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                menus,
                diets,
                mock(AuditLogApi.class),
                time,
                currentUser,
                mock(PlatformTransactionManager.class));
    }

    @Test
    void FR_046_generalShowsTheGeneralMenuOfTheComingSevenDays() {
        GeneralMenuApi api = mock(GeneralMenuApi.class);
        when(menus.getIfAvailable()).thenReturn(api);
        when(api.generalMenu(TODAY, TODAY.plusDays(6)))
                .thenReturn(List.of(new DishByMealView(TODAY, 2, "Bữa trưa", 30, "Canh bí đỏ")));

        PortalService.General general = portal.general(RESIDENT);

        assertThat(general.unavailable()).doesNotContain("menu");
        assertThat(general.menu()).hasSize(1);
        assertThat(general.menu().getFirst())
                .containsEntry("date", TODAY)
                .containsEntry("mealName", "Bữa trưa")
                .containsEntry("dishName", "Canh bí đỏ");
    }

    @Test
    void FR_046_generalMarksTheMenuUnavailableWithoutItsSource() {
        when(menus.getIfAvailable()).thenReturn(null);

        PortalService.General general = portal.general(RESIDENT);

        assertThat(general.unavailable()).contains("menu");
        assertThat(general.menu()).isEmpty();
    }

    @Test
    void FR_046_dietSectionShowsTheCurrentDietToAFamilyMemberWithEffectiveHealthRight() {
        when(relationships.rightEnabledAt(FAMILY_MEMBER, RESIDENT, FamilyRight.VIEW_HEALTH, NOW))
                .thenReturn(true);
        when(rights.rightEffective(eq(FAMILY_MEMBER), eq(RESIDENT), eq(FamilyRight.VIEW_HEALTH), any()))
                .thenReturn(true);
        DietApi api = mock(DietApi.class);
        when(diets.getIfAvailable()).thenReturn(api);
        when(api.current(RESIDENT))
                .thenReturn(Optional.of(new CurrentDietView(RESIDENT, 4, "Tiểu đường", Texture.SOFT, Set.of(), false)));

        PortalService.HealthPart part = portal.health(RESIDENT, "diet", null, null);

        assertThat(part.available()).isTrue();
        assertThat(part.items()).hasSize(1);
        assertThat(part.items().getFirst())
                .containsEntry("dietName", "Tiểu đường")
                .containsEntry("texture", "SOFT")
                .containsEntry("notAssigned", false);

        when(diets.getIfAvailable()).thenReturn(null);
        assertThat(portal.health(RESIDENT, "diet", null, null).available()).isFalse();
    }
}
