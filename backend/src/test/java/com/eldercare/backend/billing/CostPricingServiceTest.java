package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.dto.UnitPriceView;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService.BasisKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** BR-M11-02/03, FR-008/012/013: chọn căn cứ thật, không lấy giá ngày chốt. */
class CostPricingServiceTest {
    private static final LocalDate DATE = LocalDate.of(2026, 11, 6);
    private final ContractReadApi contracts = mock(ContractReadApi.class);
    private final UnitPriceApi prices = mock(UnitPriceApi.class);
    private final CostPricingService service = new CostPricingService(contracts, prices);

    @BeforeEach
    void source() {
        when(contracts.contentAtDate(11, DATE)).thenReturn(Optional.empty());
        when(prices.unitPriceAt(21, DATE, true)).thenReturn(Optional.empty());
    }

    private ContractContentView content(int version, Long addendum, String services, Map<Long, BigDecimal> recorded) {
        return new ContractContentView(
                31,
                version,
                DATE.minusDays(10),
                addendum,
                JsonMapper.builder().build().readTree("{\"services\":" + services + "}"),
                recorded);
    }

    private void contract(ContractContentView view) {
        when(contracts.contentAtDate(11, DATE)).thenReturn(Optional.of(view));
    }

    private void catalogue(String price) {
        when(prices.unitPriceAt(21, DATE, true))
                .thenReturn(Optional.of(new UnitPriceView(21, 41, new BigDecimal(price), DATE.minusDays(2), null)));
    }

    @Test
    void BR_58_signedPriceWinsOverTheCurrentCatalogue() {
        contract(content(1, null, "[{\"serviceId\":21}]", Map.of(21L, new BigDecimal("12000"))));
        var quote = service.event(11, 21, DATE, new BigDecimal("2"));
        assertThat(quote.unitPrice()).isEqualByComparingTo("12000");
        assertThat(quote.amount()).isEqualByComparingTo("24000");
        assertThat(quote.basis().kind()).isEqualTo(BasisKind.CONTRACT);
        assertThat(quote.basis().contractId()).isEqualTo(31L);
        assertThat(quote.basis().contentVersion()).isEqualTo(1);
        verifyNoInteractions(prices);
    }

    @Test
    void BR_58_packagedStatusWinsOverARecordedNonzeroUnitPrice() {
        contract(content(1, null, "[{\"serviceId\":21,\"inPackage\":true}]", Map.of(21L, new BigDecimal("12000"))));
        var quote = service.event(11, 21, DATE, BigDecimal.ONE);
        assertThat(quote.amount()).isEqualByComparingTo("0");
        assertThat(quote.unitPrice()).isEqualByComparingTo("12000");
        verifyNoInteractions(prices);
    }

    @Test
    void FR_013_quantityIsMultipliedBeforeRoundingTheFinalDongAmount() {
        catalogue("101");
        var quote = service.event(11, 21, DATE, new BigDecimal("1.50"));
        assertThat(quote.unitPrice()).isEqualByComparingTo("101");
        assertThat(quote.amount()).isEqualByComparingTo("152");
    }
}
