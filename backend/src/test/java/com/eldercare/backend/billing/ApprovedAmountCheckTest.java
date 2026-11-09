package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.procurement.domain.ApprovedAmountCheck;
import com.eldercare.backend.support.TableRows;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 019 T005 (FR-008a, BR-M11-18; quickstart Q4): số thực tế vượt số được duyệt không quá CFG-M11-07. */
class ApprovedAmountCheckTest {

    @Test
    void BR_M11_18_actualWithinApprovedPlusPercentIsAllowed() {
        // duyệt, thực tế, mức %, được phép
        TableRows.csv(
                r -> assertThat(ApprovedAmountCheck.withinLimit(
                                Long.parseLong(r[0]), Long.parseLong(r[1]), new BigDecimal(r[2])))
                        .isEqualTo(Boolean.parseBoolean(r[3])),
                "1800000, 1950000, 10, true",
                "1800000, 1980000, 10, true",
                "1800000, 1980001, 10, false",
                "1800000, 2100000, 10, false",
                "1800000, 1000000, 10, true",
                "1800000, 1800000, 0, true",
                "1800000, 1800001, 0, false",
                "1000, 1025, 2.5, true",
                "1000, 1026, 2.5, false",
                "0, 0, 10, true",
                "0, 1, 10, false");
    }

    @ParameterizedTest(name = "duyệt {0}, mức {1}% → tối đa {2}")
    @CsvSource({"1800000, 10, 1980000", "1000, 2.5, 1025", "999, 10, 1098"})
    void BR_M11_18_maximumIsRoundedDown(long approved, BigDecimal percent, long maximum) {
        assertThat(ApprovedAmountCheck.maximum(approved, percent)).isEqualTo(maximum);
    }
}
