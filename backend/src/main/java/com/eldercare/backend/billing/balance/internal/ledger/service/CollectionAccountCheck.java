package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.spi.ParameterValueCheck;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.InvalidValueException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Kiểm giá trị CFG-M11-10 trước khi 000 lưu (tên logic {@code KiemTraGiaTriThamSo}; research E-04, 017 T045): mã BIN có
 * trong danh mục ngân hàng và còn hiệu lực, số tài khoản 6 → 19 chữ số, tên chủ tài khoản không rỗng.
 */
@Component
class CollectionAccountCheck implements ParameterValueCheck {

    private final CollectionAccounts accounts;

    CollectionAccountCheck(CollectionAccounts accounts) {
        this.accounts = accounts;
    }

    @Override
    public ParameterCode code() {
        return ParameterCode.CFG_M11_10;
    }

    @Override
    public void check(JsonNode value) {
        String bin = text(value, "bin");
        if (bin == null
                || accounts.bank(bin)
                        .filter(b -> b.status() == CatalogStatus.ACTIVE)
                        .isEmpty()) {
            throw new InvalidValueException("Ngân hàng không có trong danh mục hoặc đã ngừng hiệu lực");
        }
        String account = text(value, "accountNumber");
        if (account == null || !account.matches("\\d{6,19}")) {
            throw new InvalidValueException("Số tài khoản gồm 6 đến 19 chữ số");
        }
        String holder = text(value, "accountHolder");
        if (holder == null || holder.isBlank()) {
            throw new InvalidValueException("Thiếu tên chủ tài khoản");
        }
    }

    private static String text(JsonNode value, String field) {
        JsonNode node = value == null ? null : value.get(field);
        return node == null || !node.isString() ? null : node.asString();
    }
}
