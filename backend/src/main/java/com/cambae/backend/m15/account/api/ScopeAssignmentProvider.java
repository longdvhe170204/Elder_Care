package com.cambae.backend.m15.account.api;

/**
 * SPI: gắn phạm vi cho đối tượng không gắn người cao tuổi (lịch ca, yêu cầu đổi ca, phản hồi), do module sở hữu cài
 * (FR-034, CHK049; 002 contracts/module-interfaces.md mục 1.9).
 */
public interface ScopeAssignmentProvider {

    /** Loại đối tượng, ví dụ {@code M09.SHIFT_SCHEDULE}. */
    String entityType();

    Target assignScope(long id);
}
