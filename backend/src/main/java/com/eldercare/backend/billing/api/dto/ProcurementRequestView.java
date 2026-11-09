package com.eldercare.backend.billing.api.dto;

import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.time.LocalDate;

/** 019 contracts mục 2.3 (YeuCauMuaSamView): số tiền theo đồng; approvedAmount, actualAmount rỗng khi chưa có. */
public record ProcurementRequestView(
        long requestId,
        long departmentId,
        ProcurementType type,
        ProcurementStatus status,
        long estimatedTotal,
        Long approvedAmount,
        Long actualAmount,
        LocalDate createdOn,
        LocalDate purchasedOn) {}
