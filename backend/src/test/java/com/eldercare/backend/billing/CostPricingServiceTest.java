package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.UnitPriceApi;
import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ContractContentView;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.api.dto.ServiceView;
import com.eldercare.backend.admission.api.dto.UnitPriceView;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService.BasisKind;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.CareLevel;
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

    @Test
    void FR_013_medicationUsesItsMappedServiceWithoutConvertingTheSourceQuantity() {
        when(prices.facilityMedicationServiceOf(99))
                .thenReturn(Optional.of(new ServiceView(
                        22,
                        ServiceType.FACILITY_SUPPLIED_MEDICATION,
                        "[Dịch vụ thuốc kiểm thử]",
                        "viên",
                        BillingPeriod.UNIT)));
        when(prices.unitPriceAt(22, DATE, true))
                .thenReturn(Optional.of(new UnitPriceView(22, 71, new BigDecimal("256"), DATE, null)));
        var result = service.medication(11, 99, DATE, new BigDecimal("0.125"));
        assertThat(result.service().id()).isEqualTo(22);
        assertThat(result.price().amount()).isEqualByComparingTo("32");
        verify(prices).unitPriceAt(22, DATE, true);
    }

    @Test
    void FR_012_missingMedicationServiceMappingIsNotReportedAsAMissingPriceVersion() {
        assertThatThrownBy(() -> service.medication(11, 99, DATE, BigDecimal.ONE))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error -> assertThat(error.errorCode()).isEqualTo("SOURCE_UNAVAILABLE"));
        verify(prices, never()).unitPriceAt(anyLong(), any(), anyBoolean());
    }

    @Test
    void Q_297_roomAndCareServicesAreResolvedFromTheDatedContractWithoutAddingPricesOrCosts() {
        var level = CareLevel.values()[0];
        var body = JsonMapper.builder()
                .build()
                .readTree(
                        "{\"roomTypeId\":41,\"careLevel\":\"" + level.name() + "\",\"services\":[{\"serviceId\":22}]}");
        when(contracts.contentAtDate(11, DATE))
                .thenReturn(Optional.of(new ContractContentView(31, 1, DATE, null, body, Map.of())));
        var room =
                new ServiceView(21, ServiceType.RESIDENCY_FEE, "[Phí lưu trú kiểm thử]", "tháng", BillingPeriod.MONTH);
        var care = new ServiceView(22, ServiceType.CARE, "[Chăm sóc kiểm thử]", "tháng", BillingPeriod.MONTH);
        when(prices.residencyFeeServiceOf(41)).thenReturn(Optional.of(room));
        when(prices.careServiceOf(level)).thenReturn(Optional.of(care));

        assertThat(service.residencyFeeService(11, DATE)).isEqualTo(room);
        assertThat(service.careService(11, DATE)).isEqualTo(care);
        verify(prices, never()).unitPriceAt(anyLong(), any(), anyBoolean());
    }

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
    void BR_M11_02_signedPriceWinsOverTheCurrentCatalogue() {
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
    void BR_M11_02_addendumUsesTheContentEffectiveOnTheSourceDay() {
        var before = DATE.minusDays(1);
        when(contracts.contentAtDate(11, before))
                .thenReturn(
                        Optional.of(content(1, null, "[{\"serviceId\":21}]", Map.of(21L, new BigDecimal("10000")))));
        contract(content(2, 51L, "[{\"serviceId\":21}]", Map.of(21L, new BigDecimal("15000"))));
        assertThat(service.event(11, 21, before, BigDecimal.ONE).amount()).isEqualByComparingTo("10000");
        var after = service.event(11, 21, DATE, BigDecimal.ONE);
        assertThat(after.amount()).isEqualByComparingTo("15000");
        assertThat(after.basis().contentVersion()).isEqualTo(2);
        assertThat(after.basis().addendumId()).isEqualTo(51L);
        verifyNoInteractions(prices);
    }

    @Test
    void BR_M11_02_recordedFeeIsContractualEvenOutsideTheServicesArray() {
        contract(content(1, null, "[]", Map.of(21L, new BigDecimal("300000"))));
        assertThat(service.event(11, 21, DATE, BigDecimal.ONE).amount()).isEqualByComparingTo("300000");
        verifyNoInteractions(prices);
    }

    @Test
    void BR_M11_02_unregisteredServiceUsesItsSourceDatePriceAndMarksTheVersion() {
        contract(content(1, null, "[{\"serviceId\":22,\"inPackage\":true}]", Map.of()));
        catalogue("12500");
        var quote = service.event(11, 21, DATE, new BigDecimal("2"));
        assertThat(quote.amount()).isEqualByComparingTo("25000");
        assertThat(quote.inPackage()).isFalse();
        assertThat(quote.basis().kind()).isEqualTo(BasisKind.PRICE_VERSION);
        assertThat(quote.basis().priceVersionId()).isEqualTo(41L);
        verify(prices).unitPriceAt(21, DATE, true);
    }

    @Test
    void BR_M11_03_packagedServiceCostsZeroWithoutACataloguePrice() {
        contract(content(1, null, "[{\"serviceId\":21,\"inPackage\":true}]", Map.of()));
        var quote = service.event(11, 21, DATE, new BigDecimal("3"));
        assertThat(quote.unitPrice()).isNull();
        assertThat(quote.amount()).isEqualByComparingTo("0");
        assertThat(quote.inPackage()).isTrue();
        assertThat(quote.missingUnitPrice()).isFalse();
        assertThat(quote.basis().kind()).isEqualTo(BasisKind.CONTRACT);
        verifyNoInteractions(prices);
    }

    @Test
    void BR_M11_03_packagedStatusWinsOverARecordedNonzeroUnitPrice() {
        contract(content(1, null, "[{\"serviceId\":21,\"inPackage\":true}]", Map.of(21L, new BigDecimal("12000"))));
        var quote = service.event(11, 21, DATE, BigDecimal.ONE);
        assertThat(quote.amount()).isEqualByComparingTo("0");
        assertThat(quote.unitPrice()).isEqualByComparingTo("12000");
        verifyNoInteractions(prices);
    }

    @Test
    void FR_012_missingCataloguePriceKeepsAmountUnknownRatherThanFree() {
        var quote = service.event(11, 21, DATE, new BigDecimal("2"));
        assertThat(quote.unitPrice()).isNull();
        assertThat(quote.amount()).isNull();
        assertThat(quote.missingUnitPrice()).isTrue();
        assertThat(quote.inPackage()).isFalse();
    }

    @Test
    void BR_M11_02_missingSignedPriceDoesNotSilentlyUseACataloguePrice() {
        contract(content(1, null, "[{\"serviceId\":21}]", Map.of()));
        var quote = service.event(11, 21, DATE, BigDecimal.ONE);
        assertThat(quote.unitPrice()).isNull();
        assertThat(quote.amount()).isNull();
        assertThat(quote.missingUnitPrice()).isTrue();
        assertThat(quote.basis().contractId()).isEqualTo(31L);
        verifyNoInteractions(prices);
    }

    @Test
    void FR_012_anExplicitZeroCataloguePriceIsNotMissing() {
        catalogue("0");
        var quote = service.event(11, 21, DATE, BigDecimal.ONE);
        assertThat(quote.amount()).isEqualByComparingTo("0");
        assertThat(quote.missingUnitPrice()).isFalse();
        assertThat(quote.inPackage()).isFalse();
    }

    @Test
    void FR_013_quantityIsMultipliedBeforeRoundingTheFinalDongAmount() {
        catalogue("101");
        var quote = service.event(11, 21, DATE, new BigDecimal("1.50"));
        assertThat(quote.unitPrice()).isEqualByComparingTo("101");
        assertThat(quote.amount()).isEqualByComparingTo("152");
    }

    @Test
    void BR_M11_02_unavailableContractSourceIsNotTreatedAsNoContract() {
        when(contracts.contentAtDate(11, DATE)).thenThrow(new IllegalStateException("Nguồn hợp đồng chưa sẵn sàng"));
        assertThatThrownBy(() -> service.event(11, 21, DATE, BigDecimal.ONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nguồn hợp đồng chưa sẵn sàng");
        verifyNoInteractions(prices);
    }

    @Test
    void FR_012_unavailablePriceSourceIsNotConvertedIntoAMissingPriceOrZero() {
        when(prices.unitPriceAt(21, DATE, true)).thenThrow(new IllegalStateException("Nguồn giá chưa sẵn sàng"));
        assertThatThrownBy(() -> service.event(11, 21, DATE, BigDecimal.ONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Nguồn giá chưa sẵn sàng");
    }
}
