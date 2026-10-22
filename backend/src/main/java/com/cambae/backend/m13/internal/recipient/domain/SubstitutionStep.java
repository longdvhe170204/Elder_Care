package com.cambae.backend.m13.internal.recipient.domain;

/** Một bậc của chuỗi thay mặc định (FR-010). */
public enum SubstitutionStep {
    /** Người phụ trách ca của tầng. */
    SHIFT_LEAD,
    /** Trưởng tầng được giao. */
    FLOOR_MANAGER,
    /** Người phụ trách ca đang diễn ra của tầng khi tầng chưa có Trưởng tầng (Q-90). */
    FLOOR_MANAGER_SUBSTITUTE,
    /** Quản lý viện. */
    MANAGERS
}
