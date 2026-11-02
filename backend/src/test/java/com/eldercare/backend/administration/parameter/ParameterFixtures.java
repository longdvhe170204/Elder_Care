package com.eldercare.backend.administration.parameter;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.parameter.api.dto.ParameterPermissions;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.SwitchableUser;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Người dùng và quyền giả cho test đổi tham số (000 T075 → T078): chỉ Quản lý viện xem và đổi (BR-M15-04). */
@TestConfiguration
public class ParameterFixtures {

    public static final CurrentUser MANAGER = FakePermissions.user(1, Role.NURSING_HOME_MANAGER);
    public static final CurrentUser MANAGER_2 = FakePermissions.user(2, Role.NURSING_HOME_MANAGER);
    public static final CurrentUser NURSE = FakePermissions.user(10, Role.NURSING_STAFF);
    public static final CurrentUser OFFICE = FakePermissions.user(20, Role.OFFICE_STAFF);

    @Bean
    SwitchableUser currentUser() {
        return new SwitchableUser();
    }

    @Bean
    FakePermissions permissions() {
        return new FakePermissions()
                .grant(Role.NURSING_HOME_MANAGER, ParameterPermissions.VIEW)
                .grant(Role.NURSING_HOME_MANAGER, ParameterPermissions.CHANGE);
    }
}
