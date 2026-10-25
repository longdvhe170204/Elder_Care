package com.cambae.backend.m02.internal.waitinglist.domain;

/** Tình huống đặc biệt đã xác nhận hay còn chờ xác nhận (FR-007); chỉ tình huống đã xác nhận được tính điểm. */
public enum CircumstanceStatus {
    CONFIRMED,
    PENDING_CONFIRMATION
}
