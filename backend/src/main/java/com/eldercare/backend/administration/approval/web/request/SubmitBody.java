package com.eldercare.backend.administration.approval.web.request;

import com.eldercare.backend.common.Reason;

/** Thân tùy chọn của lệnh Gửi duyệt: lý do tùy chọn (000 FR-041a (5)). */
public record SubmitBody(Reason reason) {}
