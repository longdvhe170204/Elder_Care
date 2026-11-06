package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.item.domain.CostRecalculation;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingService.*;
import com.eldercare.backend.billing.internal.pricing.service.CostPricingSnapshots;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** T013/DBR-16: snapshot dùng ghi khoản/lịch sử không đánh mất precision hoặc dấu độc lập. */
class CostPricingSnapshotsTest {
    @Test
    void FR_013_sourceQuantityIsKeptWithoutRoundingOrConversion() {
        var quote = new PriceQuote(
                new BigDecimal("256"),
                new BigDecimal("32"),
                false,
                new PriceBasis(BasisKind.PRICE_VERSION, null, null, null, 71L));
        var snapshot = CostPricingSnapshots.event(21, new BigDecimal("0.125"), quote, Map.of("sourceId", "17"));
        assertThat(snapshot.calculation().quantity()).isEqualByComparingTo("0.125");
        assertThat(snapshot.calculation().amount()).isEqualByComparingTo("32");
        assertThat(snapshot.unitPriceBasis()).containsEntry("serviceId", 21L).containsEntry("priceVersionId", 71L);
    }

    @Test
    void FR_012_missingPriceHasNoAmountAndDoesNotCreateAFakePriceBasis() {
        var snapshot =
                CostPricingSnapshots.event(21, BigDecimal.ONE, new PriceQuote(null, null, false, null), Map.of());
        assertThat(snapshot.calculation().amount()).isNull();
        assertThat(snapshot.pricingFlags()).containsExactly("MISSING_UNIT_PRICE");
        assertThat(snapshot.unitPriceBasis()).containsEntry("serviceId", 21L).doesNotContainKey("priceVersionId");
    }

    @Test
    void FR_010_zeroIncludedAmountIsDifferentFromMissingPrice() {
        var snapshot = CostPricingSnapshots.event(
                21,
                BigDecimal.ONE,
                new PriceQuote(null, BigDecimal.ZERO, true, new PriceBasis(BasisKind.CONTRACT, 31L, 2, 41L, null)),
                Map.of());
        assertThat(snapshot.calculation().amount()).isEqualByComparingTo("0");
        assertThat(snapshot.pricingFlags()).containsExactly("INCLUDED_IN_PACKAGE");
    }

    @Test
    void FR_020_replacingPricingFlagsKeepsAdvanceAndOtherSourceFlags() {
        var snapshot = CostPricingSnapshots.event(
                21,
                BigDecimal.ONE,
                new PriceQuote(
                        new BigDecimal("100"),
                        new BigDecimal("100"),
                        false,
                        new PriceBasis(BasisKind.PRICE_VERSION, null, null, null, 71L)),
                Map.of());
        assertThat(snapshot.mergeFlags(Set.of("MISSING_UNIT_PRICE", "CREATED_IN_ADVANCE", "NOT_YET_MOVED_IN")))
                .containsExactlyInAnyOrder("CREATED_IN_ADVANCE", "NOT_YET_MOVED_IN");
    }

    @Test
    void FR_020_aRecordedNullSourceFactIsPreservedInSnapshotsAndHistory() {
        var facts = new LinkedHashMap<String, Object>();
        facts.put("absenceId", null);
        var snapshot = CostPricingSnapshots.event(21, BigDecimal.ONE, new PriceQuote(null, null, false, null), facts);
        var history = new CostRecalculation(
                snapshot.calculation(),
                snapshot.calculation(),
                "PRICE",
                false,
                Map.of(),
                snapshot.unitPriceBasis(),
                facts,
                snapshot.sourceValues());
        assertThat(history.sourceBefore()).containsEntry("absenceId", null);
        assertThat(history.sourceAfter()).containsEntry("absenceId", null);
    }
}
