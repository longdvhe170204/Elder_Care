package com.cambae.backend.m02.api.dto;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import java.time.LocalDate;

/** Hồ sơ chờ cho báo cáo (016 FR-025); {@code leftOn} và {@code status} thể hiện lý do rời danh sách. */
public record WaitingEntryView(
        long id,
        String status,
        ResidenceType residenceType,
        CareLevel careLevel,
        LocalDate applicationDate,
        LocalDate leftOn) {}
