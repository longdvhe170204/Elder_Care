package com.eldercare.backend.care.web.request;

import com.eldercare.backend.common.Reason;

/** Gửi Bác sĩ duyệt (Q-447): không nhập ngày hiệu lực; lý do bắt buộc khi đã có kế hoạch trước. */
public record CarePlanSubmit(Reason changeReason) {}
