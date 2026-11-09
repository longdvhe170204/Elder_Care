package com.eldercare.backend.admission.internal.contract.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent.ServiceLine;
import com.eldercare.backend.admission.internal.contract.domain.NonStandardTerm;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 004 T030 (FR-025a, FR-036; Q-395): hàm thuần xác định điều khoản khác chuẩn: giá riêng khác phiên bản đơn giá tại ngày bắt đầu. */
class NonStandardTermDetectorTest {

    private static final Map<Long, BigDecimal> STANDARD = Map.of(1L, new BigDecimal("5000000"));

    private static ServiceLine service(long id, String price) {
        return new ServiceLine(id, false, BigDecimal.ONE, price == null ? null : new BigDecimal(price));
    }

    private static List<NonStandardTerm> detect(List<ServiceLine> services) {
        ContractContent content = new ContractContent(
                ResidenceType.LONG_TERM_RESIDENTIAL,
                LocalDate.of(2030, 1, 1),
                null,
                CareLevel.BASIC,
                null,
                null,
                BillingPeriod.MONTH,
                services,
                true,
                null,
                null,
                List.of(),
                null);
        return NonStandardTermDetector.detect(content, id -> Optional.ofNullable(STANDARD.get(id)));
    }

    @Test
    void FR_025a_noCustomPrice_isStandard() {
        assertThat(detect(List.of(service(1, null)))).isEmpty();
    }

    @Test
    void FR_025a_customPriceEqualToStandard_isStandard() {
        assertThat(detect(List.of(service(1, "5000000.00")))).isEmpty();
    }
}
