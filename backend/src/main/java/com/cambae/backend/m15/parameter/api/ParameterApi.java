package com.cambae.backend.m15.parameter.api;

import java.time.Instant;

/** Đọc tham số cấu hình lúc chạy (contracts/module-interfaces.md mục 1.4; FR-018, FR-019). */
public interface ParameterApi {

    /** Giá trị hiện hành; gọi một lần mỗi lần kiểm tra quy tắc (FR-018). */
    ParameterValue read(ParameterCode code);

    /** Giá trị đã hiện hành tại thời điểm {@code at} (FR-019). */
    ParameterValue readAt(ParameterCode code, Instant at);
}
