package com.eldercare.backend.billing.web.request;

import com.eldercare.backend.billing.internal.procurement.domain.ProcurementCommandInput;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** 019 T008: thân {@code ProcurementRequestCommand} của api-019 (glossary 25.3); trường dùng theo từng lệnh. */
public record ProcurementRequestCommandWrite(
        String reason,
        Long approvedAmount,
        Long actualAmount,
        LocalDate purchasedOn,
        String supplier,
        List<Long> receiptFileIds,
        Long discrepancyAmount,

        @Schema(allowableValues = {"ACCEPT_DISCREPANCY", "REQUEST_REFUND"})
        String resolution) {

    public ProcurementCommandInput input() {
        return new ProcurementCommandInput(
                reason,
                approvedAmount,
                actualAmount,
                purchasedOn,
                supplier,
                receiptFileIds,
                discrepancyAmount,
                resolution);
    }
}
