package com.eldercare.backend.admission.internal.contract.domain;

import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.Instant;
import java.time.LocalDate;

/** Hợp đồng Hiệu lực có ngày kết thúc hiện hành, đủ cho việc hằng ngày (không tải dịch vụ, lịch đến). */
public record ExpiringContract(
        long id,
        long residentId,
        ResidenceType residenceType,
        LocalDate currentEndDate,
        LocalDate overdueSince,
        Instant managerNotifiedOverdueAt) {}
