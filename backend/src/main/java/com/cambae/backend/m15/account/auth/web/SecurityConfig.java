package com.cambae.backend.m15.account.auth.web;

import com.cambae.backend.m15.account.auth.internal.SessionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Chuỗi lọc của API (002 research A-02): không dùng phiên servlet, form, basic; CSRF thay bằng header
 * {@code X-Requested-From} ở {@link SessionTokenFilter}. Ngoài ba đường của {@link SessionTokenFilter#PUBLIC_PATHS},
 * mọi đường cần phiên; thiếu thì 401 {@code UNAUTHORIZED}.
 */
@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, SessionService sessions, JsonMapper json) throws Exception {
        // Tạo ở đây, không đăng ký bean: bean Filter sẽ bị Spring Boot gắn thêm lần nữa vào chuỗi servlet
        SessionTokenFilter filter = new SessionTokenFilter(sessions, json);
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers(SessionTokenFilter.PUBLIC_PATHS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) ->
                        filter.writeProblem(response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Cần đăng nhập")))
                .addFilterBefore(filter, AnonymousAuthenticationFilter.class);
        return http.build();
    }
}
