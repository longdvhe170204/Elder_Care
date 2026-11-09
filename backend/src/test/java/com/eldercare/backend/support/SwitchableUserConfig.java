package com.eldercare.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Một bean {@link SwitchableUser} dùng chung cho các lớp IT (rà 2026-11-07): thay cho cấu hình lồng riêng của từng lớp
 * chỉ khai bean này, để các lớp cùng tổ hợp còn lại dùng chung một context Spring. {@code @Primary} thắng bean
 * {@code CurrentUser} giả khác nếu có; bean thật là {@code @Fallback}. {@link MySqlIntegrationTestBase} đặt lại người dùng
 * về mặc định khi sang lớp test mới, như context vừa dựng.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SwitchableUserConfig {

    @Bean
    @Primary
    SwitchableUser switchableUser() {
        return new SwitchableUser();
    }
}
