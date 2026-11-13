package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.procurement.domain.ReceiptMatch;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import com.eldercare.backend.support.TableRows;
import org.junit.jupiter.api.Test;

/**
 * 019 T005 (FR-008b, BR-M11-19, Q-294, Q-392): loại tài sản chỉ khớp khi tổng nguyên giá tài sản gắn bằng số tiền thực tế;
 * loại khác (nguyên liệu, dịch vụ, thuốc, khác) chỉ so chứng từ với số thực tế, việc so do Kế toán làm.
 */
class ReceiptMatchTest {

    @Test
    void BR_64_onlyAssetRequestsCompareLinkedTotal() {
        // loại, thực tế, tổng nguồn, khớp
        TableRows.csv(
                r -> assertThat(ReceiptMatch.matches(
                                ProcurementType.valueOf(r[0]), Long.parseLong(r[1]), Long.parseLong(r[2])))
                        .isEqualTo(Boolean.parseBoolean(r[3])),
                "ASSET, 1950000, 1950000, true",
                "ASSET, 1950000, 1900000, false",
                "ASSET, 1950000, 0, false",
                "INGREDIENT, 1950000, 0, true",
                "SERVICE, 500000, 0, true",
                "SERVICE, 500000, 300000, true",
                "MEDICATION, 300000, 0, true",
                "OTHER, 100, 0, true");
    }
}
