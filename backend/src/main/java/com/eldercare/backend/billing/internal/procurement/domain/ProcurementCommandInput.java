package com.eldercare.backend.billing.internal.procurement.domain;

import java.time.LocalDate;
import java.util.List;

/** Thân lệnh trên yêu cầu mua sắm (api-019 {@code ProcurementRequestCommand}); trường dùng theo từng lệnh. */
public record ProcurementCommandInput(
        String reason,
        Long approvedAmount,
        Long actualAmount,
        LocalDate purchasedOn,
        String supplier,
        List<Long> receiptFileIds,
        Long discrepancyAmount,
        String resolution) {

    public static final ProcurementCommandInput EMPTY =
            new ProcurementCommandInput(null, null, null, null, null, null, null, null);
}
