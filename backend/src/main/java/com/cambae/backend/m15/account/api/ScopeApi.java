package com.cambae.backend.m15.account.api;

import java.time.Instant;
import java.util.Optional;

/** Phạm vi dữ liệu; người phụ trách ca, trưởng tầng được giao lấy từ 008 (FR-026 b, CHK033, FR-049). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public interface ScopeApi {

    boolean inScope(CurrentUser user, Target target);

    /** Tài khoản người phụ trách ca của tầng/khu vực tại thời điểm đã cho. */
    Optional<Long> shiftLead(long floorAreaId, Instant at);

    /** Tài khoản trưởng tầng được giao của tầng/khu vực. */
    Optional<Long> assignedFloorManager(long floorAreaId);
}
