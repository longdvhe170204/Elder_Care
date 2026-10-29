package com.eldercare.backend.administration.account.api.spi;

import java.time.Instant;
import java.util.Optional;

/**
 * SPI: thời điểm người cao tuổi sang trạng thái cuối, cho phạm vi người thân (FR-049). Khai báo ở 002 để tránh vòng
 * M15 ↔ M02; M02 (004) cài (004 contracts mục 2.1).
 */
public interface ResidencyEndApi {

    Optional<Instant> finalStatusAt(long residentId);
}
