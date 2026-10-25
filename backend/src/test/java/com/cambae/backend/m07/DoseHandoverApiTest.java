package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cambae.backend.m07.api.dto.DosePendingHandoverView;
import com.cambae.backend.m07.api.dto.DoseStatus;
import com.cambae.backend.m07.api.dto.MedicationSource;
import com.cambae.backend.m07.internal.domain.DoseReviewStatus;
import com.cambae.backend.m07.internal.domain.MedicationDose;
import com.cambae.backend.m07.internal.persistence.DoseReviewRepository;
import com.cambae.backend.m07.internal.persistence.MedicationDoseRepository;
import com.cambae.backend.m07.internal.service.DefaultDoseHandoverService;
import com.cambae.backend.m07.internal.service.DoseAdministrationService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DoseHandoverApiTest {

    private MedicationDoseRepository doseRepository;
    private DoseReviewRepository reviewRepository;
    private DoseAdministrationService doseAdministrationService;
    private DefaultDoseHandoverService handoverService;

    @BeforeEach
    void setUp() {
        doseRepository = mock(MedicationDoseRepository.class);
        reviewRepository = mock(DoseReviewRepository.class);
        doseAdministrationService = mock(DoseAdministrationService.class);
        handoverService = new DefaultDoseHandoverService(doseRepository, reviewRepository, doseAdministrationService);
    }

    private void setId(Object entity, Long id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("LieuBanGiaoApi: Trả danh sách liều cần bàn giao trong ca trên tầng")
    void testInShift_ReturnsPendingHandoverDoses() {
        MedicationDose lateDose = new MedicationDose(
                10L,
                100L,
                1L,
                Instant.parse("2026-10-24T08:00:00Z"),
                Instant.parse("2026-10-24T07:30:00Z"),
                Instant.parse("2026-10-24T08:30:00Z"),
                BigDecimal.valueOf(1),
                "viên",
                MedicationSource.FACILITY,
                false,
                com.cambae.backend.m07.internal.domain.DoseStatus.LATE,
                1L);
        setId(lateDose, 101L);

        when(doseRepository.findByFloorAreaIdAndStatusIn(eq(1L), any())).thenReturn(List.of(lateDose));
        when(reviewRepository.findByFloorAreaIdAndStatus(1L, DoseReviewStatus.PENDING_REVIEW))
                .thenReturn(List.of());

        List<DosePendingHandoverView> views = handoverService.inShift(1L, 5L);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).prescriptionId()).isEqualTo(10L);
        assertThat(views.get(0).status()).isEqualTo(DoseStatus.LATE);
        assertThat(views.get(0).controlledSubstance()).isFalse();
    }

    @Test
    @DisplayName("LieuBanGiaoApi: Tra cứu trạng thái hiện hành của tập hợp mã liều")
    void testCurrentStatuses_ReturnsMap() {
        Instant scheduled = Instant.parse("2026-10-24T08:00:00Z");
        String doseKey = "10-202610241500";

        when(doseAdministrationService.parseDoseKey(doseKey))
                .thenReturn(new DoseAdministrationService.ParsedDoseKey(10L, scheduled));

        MedicationDose dose = new MedicationDose(
                10L,
                100L,
                1L,
                scheduled,
                Instant.parse("2026-10-24T07:30:00Z"),
                Instant.parse("2026-10-24T08:30:00Z"),
                BigDecimal.valueOf(1),
                "viên",
                MedicationSource.FACILITY,
                false,
                com.cambae.backend.m07.internal.domain.DoseStatus.ADMINISTERED,
                1L);
        when(doseRepository.findByPrescriptionIdAndScheduledTime(10L, scheduled))
                .thenReturn(Optional.of(dose));

        Map<String, DoseStatus> map = handoverService.currentStatuses(List.of(doseKey, "20-unmaterialized"));

        assertThat(map).containsEntry(doseKey, DoseStatus.ADMINISTERED);
        assertThat(map).containsEntry("20-unmaterialized", DoseStatus.SCHEDULED);
    }
}
