package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.billing.balance.internal.document.domain.VietQrPayload;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BankInfo;
import com.eldercare.backend.billing.balance.internal.ledger.domain.CollectionAccount;
import com.eldercare.backend.billing.balance.internal.ledger.domain.PaymentInstructions;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Tài khoản thu CFG-M11-10 và danh mục ngân hàng (research E-04; FR-003): chưa đặt tham số thì không có thông tin nộp
 * tiền. Ngân hàng bị tắt hiệu lực sau khi lưu vẫn dựng VietQR theo BIN đã lưu.
 */
@Service
public class CollectionAccounts {

    private final ParameterApi parameters;
    private final CatalogApi catalog;

    public CollectionAccounts(ParameterApi parameters, CatalogApi catalog) {
        this.parameters = parameters;
        this.catalog = catalog;
    }

    public Optional<CollectionAccount> account() {
        ParameterValue value = parameters.read(ParameterCode.CFG_M11_10);
        return value.isSet() ? Optional.of(value.asTable(CollectionAccount.class)) : Optional.empty();
    }

    public Optional<PaymentInstructions> instructions(String paymentCode) {
        return account()
                .map(a -> new PaymentInstructions(
                        bank(a.bin()).map(CatalogItem::name).orElse(a.bin()),
                        a.accountNumber(),
                        a.accountHolder(),
                        paymentCode,
                        VietQrPayload.of(a.bin(), a.accountNumber(), paymentCode)));
    }

    public Optional<CatalogItem<BankInfo>> bank(String bin) {
        return catalog.findByCode(BankInfo.KIND, null, bin);
    }

    public List<CatalogItem<BankInfo>> activeBanks() {
        return catalog.list(BankInfo.KIND, null, true);
    }
}
