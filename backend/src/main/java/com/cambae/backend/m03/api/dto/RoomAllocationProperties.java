package com.cambae.backend.m03.api.dto;

import com.cambae.backend.m03.api.spi.CareLevelCode;
import java.util.Set;

/** Kiểu trao đổi RoomAllocationProperties, hợp đồng 003 mục 1.2–1.9 (T003). */
public record RoomAllocationProperties(
        GenderPolicy genderPolicy,
        Set<CareLevelCode> careLevels,
        boolean longTerm,
        boolean shortTerm,
        boolean isolated,
        boolean futureAllocation,
        boolean awaitingResidentTransfer) {}
