package com.cambae.backend.m03.api.spi;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Nguồn lưu trú do 004 cài; hợp đồng 003 mục 2.1, FR-018, FR-030. */
public interface AllocationResidencySource {
    Optional<ResidencyType> residencyType(long residentId);

    Optional<CareLevelCode> careLevelFromContract(long residentId);

    Optional<CareLevelCode> careLevelFromRegistration(long residentId);

    Optional<Instant> expectedEnd(long residentId);

    HeldBed holdBedWhenAbsent(long residentId);

    boolean differentUnitPrice(long residentId, long previousRoomTypeId, long nextRoomTypeId, LocalDate date);

    @Transactional(propagation = Propagation.MANDATORY)
    void waitingRecordHoldExpired(long waitingRecordId);

    @Transactional(propagation = Propagation.MANDATORY)
    void allocated(long residentId, long allocationId);
}
