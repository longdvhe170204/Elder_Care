package com.eldercare.backend.administration.account.internal.permission.config;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.internal.permission.service.FieldGroupSerializerModifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/** Đăng ký giới hạn trường vào {@code JsonMapper} của Spring Boot (bean {@code JacksonModule} được nạp tự động). */
@Configuration
class FieldVisibilityJacksonConfig {

    @Bean
    JacksonModule fieldGroupModule(
            ObjectProvider<CurrentUser> currentUser, ObjectProvider<FieldVisibilityApi> visibility) {
        return new SimpleModule("field-group")
                .setSerializerModifier(new FieldGroupSerializerModifier(currentUser, visibility));
    }
}
