package com.cambae.backend.m03.web.request;

import jakarta.validation.constraints.Size;

/** T060: các dấu xác nhận chỉ bắt buộc khi nguồn007 yêu cầu. */
public record BedReadinessWrite(
        Boolean disinfected,
        Boolean ppeUsed,
        @Size(max = 2000) String note) {}
