package com.eldercare.backend.common.offline;

/** Kết quả đồng bộ một bản ghi trả cho app (R-13, BR-M15-20). */
public enum SyncResult {
    ACCEPTED,
    ACCEPTED_WITH_FLAG,
    PENDING_REVIEW,
    REJECTED
}
