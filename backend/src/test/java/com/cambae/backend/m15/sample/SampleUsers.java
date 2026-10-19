package com.cambae.backend.m15.sample;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.support.FakePermissions;
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
