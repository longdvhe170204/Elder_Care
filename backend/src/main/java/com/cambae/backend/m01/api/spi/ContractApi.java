package com.cambae.backend.m01.api.spi;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidenceTypePeriod;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** SPI: M02 (004) cài (FR-009, FR-052, FR-053). */
public interface ContractApi {

    Optional<ResidenceType> currentResidenceType(long residentId, LocalDate date);

    List<ResidenceTypePeriod> residenceTypeHistory(long residentId);
}
