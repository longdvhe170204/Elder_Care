package com.eldercare.backend.administration.account.api.spi;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * SPI: phân bổ giường cho phạm vi dữ liệu (FR-029, FR-030). Khai báo ở 002 để tránh vòng M15 ↔ M03; M03 (003) cài
 * (002 contracts/module-interfaces.md mục 2; 003 contracts mục 1.1).
 */
public interface BedAllocationApi {

    /** Phân bổ đang hiệu lực của người cao tuổi tại thời điểm đã cho. */
    Optional<BedAllocationView> openAt(long residentId, Instant at);

    /** Người cao tuổi có phân bổ đang hiệu lực trong tập tầng/khu vực, phòng của bộ lọc tại thời điểm đã cho. */
    Set<Long> residentsIn(ScopeFilter scope, Instant at);
}
