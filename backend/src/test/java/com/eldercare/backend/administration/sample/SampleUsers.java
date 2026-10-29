package com.eldercare.backend.administration.sample;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.FakePermissions;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Người dùng hiện tại cho test dùng dữ liệu mẫu: Quản lý viện (cấu hình danh mục). */
@TestConfiguration
public class SampleUsers {

    @Bean
    CurrentUser currentUser() {
        return FakePermissions.user(1, Role.NURSING_HOME_MANAGER);
    }
}
