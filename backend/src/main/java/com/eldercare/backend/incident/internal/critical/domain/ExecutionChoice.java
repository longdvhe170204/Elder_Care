package com.eldercare.backend.incident.internal.critical.domain;

/** Lựa chọn được thực hiện khi nguy kịch (FR-050d), cùng tên với lựa chọn của phiếu nguyện vọng 001. */
public enum ExecutionChoice {
    TRANSFER_TO_HOSPITAL,
    RETURN_HOME,
    STAY_PALLIATIVE
}
