package com.eldercare.backend.care.web.request;

import com.eldercare.backend.common.Reason;
import java.time.LocalDate;

/** Ngày đề xuất chỉ được nhập khi gửi duyệt. */
public record CarePlanSubmit(LocalDate effectiveFrom, Reason changeReason) {}
