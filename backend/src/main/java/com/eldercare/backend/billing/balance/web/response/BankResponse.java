package com.eldercare.backend.billing.balance.web.response;

import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BankInfo;

/** Schema {@code Bank} của api-017: mã BIN NAPAS, tên viết tắt, tên đầy đủ. */
public record BankResponse(String bin, String shortName, String fullName) {

    public static BankResponse of(CatalogItem<BankInfo> item) {
        return new BankResponse(
                item.code(),
                item.name(),
                item.attributes() == null ? null : item.attributes().fullName());
    }
}
