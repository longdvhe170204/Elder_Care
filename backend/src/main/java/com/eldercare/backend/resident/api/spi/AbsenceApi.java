package com.eldercare.backend.resident.api.spi;

import com.eldercare.backend.resident.api.dto.OpenAbsence;
import java.util.Optional;

/** SPI: M02 (004) cài (FR-048, FR-049); 001 dùng cho nhắc quá hạn Tạm vắng (FR-050). */
public interface AbsenceApi {

    Optional<OpenAbsence> open(long residentId);
}
