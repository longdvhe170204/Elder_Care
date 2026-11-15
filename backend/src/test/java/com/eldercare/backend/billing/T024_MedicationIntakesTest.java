package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.procurement.service.MedicationIntakes;
import com.eldercare.backend.medication.api.dto.IntakeResultView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 019 T024 (FR-008d "Đã nhận", Q-337): bản cài {@code ProcurementIntakePort} của M11 lấy id dòng có ít nhất một lần
 * tiếp nhận Đạt từ {@code medication.api.ProcurementIntakeApi}; dòng chỉ có lần Không đạt thì không tính.
 */
class T024_MedicationIntakesTest {

    private static IntakeResultView intake(long id, long line, boolean passed) {
        return new IntakeResultView(id, line, "Amlodipin", BigDecimal.TEN, passed, null, Instant.EPOCH);
    }

    @Test
    void T024_passedLineIds_onlyLinesWithAPassedIntake() {
        var port = new MedicationIntakes(request ->
                request == 9 ? List.of(intake(1, 100, false), intake(2, 100, true), intake(3, 101, false)) : List.of());

        assertThat(port.passedLineIds(9)).containsExactly(100L);
        assertThat(port.passedLineIds(8)).isEmpty();
    }
}
