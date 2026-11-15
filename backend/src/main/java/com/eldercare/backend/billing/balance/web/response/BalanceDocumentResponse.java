package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.billing.api.dto.BalanceDocumentType;
import com.eldercare.backend.billing.balance.internal.document.service.PaymentNoticeService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Schema {@code BalanceDocument} của api-017: một chứng từ trên cổng người thân. */
@Schema(name = "BalanceDocument")
public record BalanceDocumentResponse(
        BalanceDocumentType type, long id, String code, LocalDate date, String period, List<String> formats) {

    public static BalanceDocumentResponse of(PaymentNoticeService.PortalDocument d) {
        return new BalanceDocumentResponse(d.type(), d.id(), d.code(), d.date(), d.period(), d.formats());
    }
}
