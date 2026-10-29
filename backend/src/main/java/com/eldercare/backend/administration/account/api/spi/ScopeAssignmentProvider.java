package com.eldercare.backend.administration.account.api.spi;

import com.eldercare.backend.administration.account.api.dto.Target;

/**
 * SPI: gắn phạm vi cho đối tượng không gắn người cao tuổi (lịch ca, yêu cầu đổi ca, phản hồi), do module sở hữu cài
 * (FR-034, CHK049; 002 contracts/module-interfaces.md mục 1.9).
 */
public interface ScopeAssignmentProvider {

    /** Loại đối tượng, ví dụ {@code M09.SHIFT_SCHEDULE}. */
    String entityType();

    Target assignScope(long id);
}
