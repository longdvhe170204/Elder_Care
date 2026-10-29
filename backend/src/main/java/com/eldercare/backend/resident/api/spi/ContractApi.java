package com.eldercare.backend.resident.api.spi;

import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidenceTypePeriod;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** SPI: M02 (004) cài (FR-009, FR-052, FR-053). */
public interface ContractApi {

    Optional<ResidenceType> currentResidenceType(long residentId, LocalDate date);

    List<ResidenceTypePeriod> residenceTypeHistory(long residentId);
}
