package com.eldercare.backend.medication.internal.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.spi.MedicationChecklistPort;
import com.eldercare.backend.medication.api.MedicationScheduleApi;
import com.eldercare.backend.medication.api.dto.DoseDisplayView;
import com.eldercare.backend.medication.api.dto.DoseStatus;
import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.api.dto.MedicationSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MedicationChecklistPortAdapterTest {

    @Test
    void FR_029_checklistReadsDosesInShiftAndKeepsMedicationSourceFields() {
        var from = Instant.parse("2026-11-03T00:00:00Z");
        var to = from.plusSeconds(3600);
        var summary = new MedicationSummary(4, "Thuốc mẫu", "", "10 mg", "");
        var schedule = new MedicationScheduleApi() {
            @Override
            public List<DoseDisplayView> byResident(long residentId, Instant start, Instant end) {
                assertThat(residentId).isEqualTo(7);
                assertThat(start).isEqualTo(from);
                assertThat(end).isEqualTo(to);
                return List.of(
                        new DoseDisplayView(
                                "in",
                                7,
                                9,
                                from,
                                from,
                                to,
                                DoseStatus.DUE,
                                true,
                                true,
                                true,
                                true,
                                MedicationSource.FAMILY_SENT,
                                Optional.of(12L),
                                Optional.of(summary)),
                        new DoseDisplayView(
                                "after",
                                7,
                                9,
                                to,
                                to,
                                to,
                                DoseStatus.SCHEDULED,
                                false,
                                false,
                                false,
                                false,
                                MedicationSource.FACILITY,
                                Optional.empty(),
                                Optional.empty()));
            }

            @Override
            public Set<Long> staffRecordedDose(long residentId, Instant start, Instant end) {
                throw new AssertionError();
            }
        };

        var port = MedicationChecklistPort.class.cast(new MedicationChecklistPortAdapter(schedule));

        assertThat(port.inRange(List.of(7L), from, to))
                .containsExactly(new MedicationChecklistPort.DoseView(
                        "in",
                        7,
                        9,
                        from,
                        from,
                        to,
                        "DUE",
                        true,
                        true,
                        true,
                        true,
                        "FAMILY_SENT",
                        Optional.of(12L),
                        Optional.of(new MedicationChecklistPort.MedicationSummary("Thuốc mẫu"))));
    }
}
