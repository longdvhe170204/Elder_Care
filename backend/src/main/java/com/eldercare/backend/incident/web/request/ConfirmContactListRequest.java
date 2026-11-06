package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.common.Reason;

/** Thân lệnh xác nhận danh sách tiếp xúc: ≥ 1 người, hoặc {@code noContacts} kèm lý do (bảng FR-061). */
public record ConfirmContactListRequest(Boolean noContacts, Reason reason) {}
