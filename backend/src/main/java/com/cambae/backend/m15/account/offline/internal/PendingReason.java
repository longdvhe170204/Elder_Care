package com.cambae.backend.m15.account.offline.internal;

/** Lý do chờ xem lại theo FR-041c (a), (b), (c) (data-model mục 5 {@code ly_do_cho}). */
public enum PendingReason {
    LATER_THAN_SYNC,
    EARLIER_THAN_LAST_CONNECTION,
    EXCEEDS_CFG_M15_08
}
