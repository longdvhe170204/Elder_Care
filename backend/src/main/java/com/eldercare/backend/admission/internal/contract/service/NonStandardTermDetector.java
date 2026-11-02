package com.eldercare.backend.admission.internal.contract.service;

import com.eldercare.backend.admission.internal.contract.domain.ContractContent;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent.ServiceLine;
import com.eldercare.backend.admission.internal.contract.domain.NonStandardTerm;
import com.eldercare.backend.admission.internal.contract.domain.NonStandardTerm.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Xác định điều khoản khác chuẩn của hợp đồng Nháp (FR-025a; research T-04; hàm thuần): giá riêng khác phiên bản đơn giá tại ngày
 * bắt đầu (FR-036). Q-395 bỏ điều khoản ghi đè chính sách vắng. Trường {@code bedHoldPolicy} dạng văn bản chỉ mô tả, không so.
 */
final class NonStandardTermDetector {

    private NonStandardTermDetector() {}

    /** @param standardPrice đơn giá chuẩn của dịch vụ tại ngày bắt đầu hợp đồng, rỗng nếu chưa có phiên bản hiệu lực */
    static List<NonStandardTerm> detect(ContractContent content, LongFunction<Optional<BigDecimal>> standardPrice) {
        List<NonStandardTerm> terms = new ArrayList<>();
        for (ServiceLine line : content.services()) {
            if (line.customUnitPrice() == null) {
                continue;
            }
            Optional<BigDecimal> standard = standardPrice.apply(line.serviceId());
            if (standard.isPresent() && standard.get().compareTo(line.customUnitPrice()) == 0) {
                continue;
            }
            terms.add(new NonStandardTerm(
                    Type.PRICE,
                    "Đơn giá riêng của dịch vụ " + line.serviceId(),
                    standard.map(NonStandardTermDetector::plain).orElse(null),
                    plain(line.customUnitPrice())));
        }
        return terms;
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
