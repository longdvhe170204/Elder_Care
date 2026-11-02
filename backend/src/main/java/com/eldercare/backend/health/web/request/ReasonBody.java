package com.eldercare.backend.health.web.request;

import com.eldercare.backend.common.Reason;

/** Lý do bắt buộc của lệnh Ngừng hiệu lực. */
public record ReasonBody(Reason reason) {}
