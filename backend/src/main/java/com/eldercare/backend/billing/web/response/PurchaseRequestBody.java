package com.eldercare.backend.billing.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.billing.internal.purchase.domain.*;
import com.eldercare.backend.billing.web.request.NewPurchaseRequest.Requester;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PurchaseRequestBody(
        long residentId,
        Long catalogItemId,
        String description,
        BigDecimal quantity,
        long estimatedAmount,
        String reason,
        Requester requester,
        long id,
        String status,
        Long allowedAmount,
        boolean additionalConsentPending,
        boolean insufficientBalance,
        List<PurchaseDecision> decisions) {
    public static PurchaseRequestBody of(SimpleRecord<PurchaseDetails> row, List<PurchaseDecision> decisions) {
        var c = row.content();
        return new PurchaseRequestBody(
                row.residentId(),
                c.catalogItemId(),
                c.description(),
                c.quantity(),
                c.consent().estimatedAmount(),
                c.reason(),
                new Requester(c.requesterType(), c.requesterId()),
                row.id(),
                row.status(),
                c.consent().allowedAmount(),
                c.consent().additionalPending(),
                c.insufficientBalance(),
                decisions);
    }
}
