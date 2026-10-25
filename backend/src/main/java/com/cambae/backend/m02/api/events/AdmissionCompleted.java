package com.cambae.backend.m02.api.events;

import com.cambae.backend.m01.api.dto.ResidenceType;
import java.time.LocalDate;

/** Hoàn tất tiếp nhận; 018 nghe để khai báo tạm trú (FR-033b). {@code expectedEndDate} rỗng khi hợp đồng không có ngày kết thúc. */
public record AdmissionCompleted(
        long residentId,
        long contractId,
        ResidenceType residenceType,
        LocalDate startDate,
        LocalDate expectedEndDate) {}
