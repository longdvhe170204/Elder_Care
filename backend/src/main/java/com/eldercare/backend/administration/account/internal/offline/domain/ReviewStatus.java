package com.eldercare.backend.administration.account.internal.offline.domain;

/** Trạng thái bản ghi chờ xem lại: {@code CHO_XEM_LAI → DA_CHAP_NHAN / DA_TU_CHOI} (data-model mục 5). */
public enum ReviewStatus {
    PENDING_REVIEW,
    ACCEPTED,
    REJECTED
}
