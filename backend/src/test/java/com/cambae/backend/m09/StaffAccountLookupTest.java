package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cambae.backend.common.Role;
import com.cambae.backend.m09.api.ShiftApi;
import com.cambae.backend.m09.staff.internal.domain.StaffProfile;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m09.staff.internal.service.StaffEmploymentEffects;
import com.cambae.backend.m09.staff.internal.service.StaffService;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.AccountView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** FR-001/001a: ô chọn phụ trách đọc liên kết tài khoản thật qua API 002, không phụ thuộc cột cục bộ chưa nối. */
class StaffAccountLookupTest {
    private StaffStore staff;
    private StaffEmploymentEffects effects;
    private StaffService service;
    private final StaffProfile profile = new StaffProfile(
            41,
            "[Nhân viên kiểm thử]",
            "Nhân viên chăm sóc",
            null,
            null,
            null,
            null,
            "WORKING",
            null,
            "MANUAL",
            null,
            3L,
            0);

    @BeforeEach
    void setUp() {
        staff = mock(StaffStore.class);
        effects = mock(StaffEmploymentEffects.class);
        var user = mock(CurrentUser.class);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        var beans = new StaticListableBeanFactory(Map.of("currentUser", user));
        service = new StaffService(
                staff,
                null,
                null,
                beans.getBeanProvider(CurrentUser.class),
                beans.getBeanProvider(ShiftApi.class),
                null,
                null,
                null,
                effects);
        when(staff.find(41)).thenReturn(Optional.of(profile));
        when(staff.all()).thenReturn(List.of(profile));
    }

    @Test
    void FR_001a_staffListIncludesTheAccountCreatedByModule002() {
        when(effects.account(41)).thenReturn(Optional.of(account()));

        var result = service.list(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().accountId()).isEqualTo(77L);
        assertThat(result.getFirst())
                .usingRecursiveComparison()
                .ignoringFields("accountId")
                .isEqualTo(profile);
    }

    @Test
    void FR_001_staffDetailIncludesTheAccountCreatedByModule002() {
        when(effects.account(41)).thenReturn(Optional.of(account()));

        assertThat(service.get(41).accountId()).isEqualTo(77L);
    }

    @Test
    void FR_001_staffWithoutAnAccountKeepsTheNullableLink() {
        when(effects.account(41)).thenReturn(Optional.empty());

        assertThat(service.get(41)).isEqualTo(profile);
        assertThat(service.list(null, null, null)).containsExactly(profile);
    }

    private AccountView account() {
        return new AccountView(77, AccountType.STAFF, AccountStatus.ACTIVE, Set.of(Role.CAREGIVER), 41L);
    }
}
