package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.schedule.service.CalculatedDoseSlot;
import com.eldercare.backend.medication.web.response.DoseBodies.DoseBody;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * FR-026, FR-018 (chủ dự án duyệt 2026-11-08, "Đổi api" api-006): thẻ Phát thuốc cần tên thuốc, liều lượng, đơn vị,
 * đường dùng nên {@code Dose} mang {@code medicationName}, {@code dosage}, {@code dosageUnit}, {@code route}.
 */
class FR_026_DoseBodyDisplayFieldsTest {

    @Test
    void FR_026_scheduledSlotBodyCarriesMedicationDisplayFields() {
        Instant at = Instant.parse("2026-11-09T01:00:00Z");
        CalculatedDoseSlot slot = new CalculatedDoseSlot(
                "7-202611090800",
                7L,
                9L,
                100L,
                "Metformin 500mg",
                new BigDecimal("1.50"),
                "viên",
                "Uống",
                MedicationSource.FACILITY,
                false,
                at,
                at.minusSeconds(1800),
                at.plusSeconds(1800),
                DoseStatus.UPCOMING,
                null,
                false,
                null);

        DoseBody body = DoseBody.of(slot);

        assertThat(body.medicationName()).isEqualTo("Metformin 500mg");
        assertThat(body.dosage()).isEqualByComparingTo("1.50");
        assertThat(body.dosageUnit()).isEqualTo("viên");
        assertThat(body.route()).isEqualTo("Uống");
    }
}
