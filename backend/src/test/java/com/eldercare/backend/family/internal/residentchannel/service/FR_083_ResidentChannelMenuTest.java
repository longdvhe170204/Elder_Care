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
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.visit.service.VisitQueries;
import com.eldercare.backend.nutrition.api.ExpectedMealApi;
import com.eldercare.backend.nutrition.api.dto.ExpectedPortionView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 012 FR-083 (b), FR-084; 011 FR-063 (Q-366, Q-367): kênh người cao tuổi đọc suất ăn dự kiến của chính mình trong tuần
 * (thứ hai → chủ nhật) qua {@code nutrition.api.ExpectedMealApi}: ngày, bữa, tên món, cờ món thay thế; không có chế độ ăn,
 * dị ứng. 011 chưa có thì mục báo "chưa có dữ liệu".
 */
class FR_083_ResidentChannelMenuTest {

    static final long RESIDENT = 7;
    /** Thứ ba; tuần là 2026-11-09 (thứ hai) → 2026-11-15 (chủ nhật). */
    static final LocalDate TODAY = LocalDate.of(2026, 11, 10);

    static final LocalDate MONDAY = LocalDate.of(2026, 11, 9);

    @SuppressWarnings("unchecked")
    static ResidentChannelService service(ExpectedMealApi api) {
        ObjectProvider<ExpectedMealApi> meals = mock(ObjectProvider.class);
        when(meals.getIfAvailable()).thenReturn(api);
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
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                meals,
                permissions,
                time,
                current);
    }

    @Test
    void FR_083_menuShowsOwnExpectedPortionsOfTheWeekWithSubstitutes() {
        ExpectedMealApi api = mock(ExpectedMealApi.class);
        when(api.ofResident(RESIDENT, MONDAY, MONDAY.plusDays(6)))
                .thenReturn(List.of(
                        new ExpectedPortionView(TODAY, 2, "Bữa trưa", "Cá hấp", false),
                        new ExpectedPortionView(TODAY, 2, "Bữa trưa", "Đậu phụ sốt cà", true)));

        var menu = service(api).menu(null);

        assertThat(menu.available()).isTrue();
        assertThat(menu.items())
                .containsExactly(
                        new ResidentChannelService.ChannelMeal(TODAY, "Bữa trưa", "Cá hấp", false),
                        new ResidentChannelService.ChannelMeal(TODAY, "Bữa trưa", "Đậu phụ sốt cà", true));
    }

    @Test
    void FR_083_menuOfAnotherWeekStartsOnItsMonday_andSaysSoWithoutSource() {
        ExpectedMealApi api = mock(ExpectedMealApi.class);
        LocalDate nextMonday = MONDAY.plusDays(7);
        when(api.ofResident(RESIDENT, nextMonday, nextMonday.plusDays(6)))
                .thenReturn(List.of(new ExpectedPortionView(nextMonday, 1, "Bữa sáng", "Cháo yến mạch", false)));

        assertThat(service(api).menu(nextMonday.plusDays(3)).items())
                .extracting(ResidentChannelService.ChannelMeal::name)
                .containsExactly("Cháo yến mạch");

        var missing = service(null).menu(null);
        assertThat(missing.available()).isFalse();
        assertThat(missing.items()).isEmpty();
    }
}
