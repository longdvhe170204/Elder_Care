package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.procurement.domain.ReceiptMatch;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 019 T005 (FR-008b, BR-M11-19, Q-294, Q-392): loại tài sản chỉ khớp khi tổng nguyên giá tài sản gắn bằng số tiền thực tế;
 * loại khác (nguyên liệu, dịch vụ, thuốc, khác) chỉ so chứng từ với số thực tế, việc so do Kế toán làm.
 */
class ReceiptMatchTest {

    @ParameterizedTest(name = "{0}: thực tế {1}, tổng nguồn {2} → {3}")
    @CsvSource({
        "ASSET, 1950000, 1950000, true",
        "ASSET, 1950000, 1900000, false",
        "ASSET, 1950000, 0, false",
        "INGREDIENT, 1950000, 0, true",
        "SERVICE, 500000, 0, true",
        "SERVICE, 500000, 300000, true",
        "MEDICATION, 300000, 0, true",
        "OTHER, 100, 0, true"
    })
    void BR_M11_19_onlyAssetRequestsCompareLinkedTotal(
            ProcurementType type, long actual, long linkedTotal, boolean matches) {
        assertThat(ReceiptMatch.matches(type, actual, linkedTotal)).isEqualTo(matches);
    }
}
