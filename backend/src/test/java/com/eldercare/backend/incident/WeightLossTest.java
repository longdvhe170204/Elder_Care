package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.trend.domain.WeightLoss;
import com.eldercare.backend.incident.internal.trend.domain.WeightLoss.Reading;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR-039a (CHK042): sụt cân so với lần cao nhất trong cửa sổ; chuỗi 50,0 → 48,5 → 47,4 kg với ngưỡng 5%. */
class WeightLossTest {
    private static final BigDecimal FIVE = new BigDecimal("5");

    private static Reading r(long id, String kg) {
        return new Reading(id, new BigDecimal(kg));
    }

    @Test
    void FR_039a_secondWeighingBelowThresholdDoesNotTrigger() {
        assertThat(WeightLoss.detect(r(2, "48.5"), List.of(r(1, "50.0")), FIVE)).isEmpty();
    }

    @Test
    void FR_039a_thirdWeighingReachesThresholdAgainstHighest() {
        var detection = WeightLoss.detect(r(3, "47.4"), List.of(r(1, "50.0"), r(2, "48.5")), FIVE)
                .orElseThrow();

        assertThat(detection.highest().recordId()).isEqualTo(1);
        assertThat(detection.dropPercent()).isEqualByComparingTo("5.2");
    }
}
