package com.cambae.backend.m15.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Lỗi chưa xử lý được Tomcat chuyển sang {@code /error}; lượt đó không có phiên (bộ lọc phiên chỉ chạy một lần mỗi yêu
 * cầu) nên trước đây chuỗi lọc trả 401 "Cần đăng nhập", che lỗi thật (dashboard với tài khoản demo, 2026-10-26).
 */
@AutoConfigureMockMvc
class ErrorDispatchIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Test
    void errorDispatch_keepsOriginalStatus_notUnauthorized() throws Exception {
        mvc.perform(get("/error").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
                    request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/v1/dashboard");
                    return request;
                }))
                .andExpect(status().isInternalServerError());
    }
}
