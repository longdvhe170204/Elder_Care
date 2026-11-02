package com.eldercare.backend.administration.account.internal.auth.config;

import com.eldercare.backend.administration.account.internal.auth.service.FirstManagerBootstrap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Chạy {@link FirstManagerBootstrap} một lần khi cài đặt (002 T016; research A-03): mở ứng dụng với
 * {@code --eldercare.bootstrap-manager.run-once=true --eldercare.bootstrap-manager.username=...
 * --eldercare.bootstrap-manager.full-name=...}. Mật khẩu tạm in ra màn hình một lần, không ghi vào log ứng dụng
 * (backend/docs/operations.md mục 2a).
 */
@Component
class FirstManagerBootstrapRunner {

    private final FirstManagerBootstrap bootstrap;
    private final boolean runOnce;
    private final String username;
    private final String fullName;

    FirstManagerBootstrapRunner(
            FirstManagerBootstrap bootstrap,
            @Value("${eldercare.bootstrap-manager.run-once:false}") boolean runOnce,
            @Value("${eldercare.bootstrap-manager.username:}") String username,
            @Value("${eldercare.bootstrap-manager.full-name:}") String fullName) {
        this.bootstrap = bootstrap;
        this.runOnce = runOnce;
        this.username = username;
        this.fullName = fullName;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        if (!runOnce) {
            return;
        }
        if (username.isBlank() || fullName.isBlank()) {
            throw new IllegalStateException(
                    "Cần eldercare.bootstrap-manager.username và eldercare.bootstrap-manager.full-name");
        }
        FirstManagerBootstrap.Result result = bootstrap.run(username.strip(), fullName.strip());
        System.out.println("Đã tạo tài khoản Quản lý viện đầu tiên: " + result.username());
        System.out.println("Mật khẩu tạm (buộc đổi ở lần đăng nhập đầu): " + result.temporaryPassword());
    }
}
