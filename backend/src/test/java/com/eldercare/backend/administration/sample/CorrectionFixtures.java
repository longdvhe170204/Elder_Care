package com.eldercare.backend.administration.sample;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.CorrectionPermissions;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.SwitchableUser;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Người dùng và bản giả quyền cho test đính chính: tầng 2 có người phụ trách ca (12) và trưởng tầng được giao (13);
 * cách C: nhân viên lập được yêu cầu {@code M15.CORRECTION}, Quản lý viện (1) duyệt.
 */
@TestConfiguration
public class CorrectionFixtures {

    public static final long FLOOR = 2;
    public static final CurrentUser MANAGER = FakePermissions.user(1, Role.NURSING_HOME_MANAGER);
    public static final CurrentUser NURSE_A = FakePermissions.user(10, Role.NURSING_STAFF);
    public static final CurrentUser NURSE_B = FakePermissions.user(11, Role.NURSING_STAFF);
    public static final CurrentUser SHIFT_LEAD = FakePermissions.user(12, Role.NURSING_STAFF);
    public static final CurrentUser FLOOR_MANAGER = FakePermissions.user(13, Role.FLOOR_MANAGER);
    public static final CurrentUser OFFICE_A = FakePermissions.user(20, Role.OFFICE_STAFF);
    public static final CurrentUser OFFICE_B = FakePermissions.user(21, Role.OFFICE_STAFF);

    @Bean
    SwitchableUser currentUser() {
        return new SwitchableUser();
    }

    @Bean
    FakePermissions permissions() {
        return new FakePermissions()
                .account(MANAGER)
                .account(NURSE_A)
                .account(NURSE_B)
                .account(SHIFT_LEAD)
                .account(FLOOR_MANAGER)
                .account(OFFICE_A)
                .account(OFFICE_B)
                .shiftLead(FLOOR, SHIFT_LEAD.accountId())
                .floorManager(FLOOR, FLOOR_MANAGER.accountId())
                .grant(Role.OFFICE_STAFF, CorrectionPermissions.REQUEST)
                .grant(Role.NURSING_STAFF, CorrectionPermissions.REQUEST)
                .grant(Role.NURSING_HOME_MANAGER, CorrectionPermissions.APPROVE);
    }
}
