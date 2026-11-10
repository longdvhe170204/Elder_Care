package com.eldercare.backend.care;

import static com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.minimumItems;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.support.TableRows;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SoMucToiThieuTest {
    @Test
    void FR_059_roundsUpAndRequiresAtLeastOneItemWhenTasksAreEligible() {
        TableRows.csv(
                row -> assertThat(minimumItems(Integer.parseInt(row[0]), new BigDecimal(row[1])))
                        .isEqualTo(Integer.parseInt(row[2])),
                "0,5,0",
                "1,0.1,1",
                "20,5,1",
                "21,5,2",
                "40,5,2",
                "9,12.5,2");
    }
}
