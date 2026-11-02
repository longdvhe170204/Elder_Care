package com.eldercare.backend.admission.api.dto;

import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.LocalDate;

/**
 * Thông tin cho bảng quyết toán của 017 (017 FR-022, FR-034). {@code deathDate} chỉ có khi {@code endCase} là {@code
 * DEATH}; {@code contractId}, {@code startDate} rỗng khi người đó không có hợp đồng nào. Provisional: chờ 017 xác nhận.
 */
public record SettlementInfoView(
        ResidenceType residenceType,
        Long contractId,
        LocalDate startDate,
        LocalDate expectedEndDate,
        EndCase endCase,
        LocalDate deathDate) {}
