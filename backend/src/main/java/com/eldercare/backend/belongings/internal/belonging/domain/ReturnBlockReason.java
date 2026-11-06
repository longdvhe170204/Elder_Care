package com.eldercare.backend.belongings.internal.belonging.domain;

/** Lý do chặn lần trả (data-model mục 5 {@code lan_chan_tra.ly_do}; FR-016, FR-016a, FR-017). */
public enum ReturnBlockReason {
    ID_DOCUMENT_MISMATCH,
    NOT_ENTITLED_TO_RECEIVE,
    INVALID_RECIPIENT_CONFIRMATION,
    NURSING_STAFF_VALUABLE_ITEM
}
