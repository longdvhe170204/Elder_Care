package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.medication.internal.domain.Medication;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.persistence.MedicationRepository;
import com.eldercare.backend.medication.internal.persistence.PrescriptionRepository;
import com.eldercare.backend.medication.internal.service.AllergyItemView;
import com.eldercare.backend.medication.internal.service.HealthAllergyPort;
import com.eldercare.backend.medication.internal.service.PrescriptionAllergyListener;
import com.eldercare.backend.resident.api.events.AllergyRecorded;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Kiểm thử tích hợp FR-012: Kiểm tra lại đơn thuốc khi có dị ứng mới được ghi nhận ở Module 001.
 */
class FR_012_NewAllergyIT extends MySqlIntegrationTestBase {

    @Autowired
    private PrescriptionAllergyListener allergyListener;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private TestAlertCollector alertCollector;

    @Autowired
    private TestHealthAllergyPort testHealthAllergyPort;

    @Autowired
    private JdbcTemplate jdbc;

    private Long residentId = 7701L;
    private Long medId = 7701L;
    private Long allergenId = 202L;

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        TestHealthAllergyPort testHealthAllergyPort() {
            return new TestHealthAllergyPort();
        }

        @Bean
        TestAlertCollector testAlertCollector() {
            return new TestAlertCollector();
        }
    }

    static class TestHealthAllergyPort implements HealthAllergyPort {
        final AtomicBoolean checkedCalled = new AtomicBoolean(false);

        @Override
        public List<AllergyItemView> getActiveAllergies(long residentId) {
            return List.of();
        }

        @Override
        public void markAllergyChecked(long allergyId, String module) {
            checkedCalled.set(true);
        }
    }

    static class TestAlertCollector {
        final List<MedicationAlertRequestedEvent> alerts = new ArrayList<>();

        @EventListener
        public void handle(MedicationAlertRequestedEvent event) {
            alerts.add(event);
        }
    }

    @BeforeEach
    void setUp() {
        alertCollector.alerts.clear();
        testHealthAllergyPort.checkedCalled.set(false);

        jdbc.update(
                "INSERT INTO resident_medication (resident_id, reconciliation_due_at, reconciliation_reason, reconciliation_warned_at, version) "
                        + "VALUES (?, NULL, NULL, NULL, 0) ON DUPLICATE KEY UPDATE version = version",
                residentId);

        if (!medicationRepository.existsById(medId)) {
            Medication med =
                    new Medication("Aspirin 81mg", "81mg", "Tablet", "Uống", "Viên", 30, 30, false, Set.of(allergenId));
            med = medicationRepository.save(med);
            medId = med.getId();
        }

        // Tạo một đơn thuốc đang hiệu lực chứa allergenId
        Prescription p = new Prescription(
                residentId,
                medId,
                new BigDecimal("1.0"),
                "Viên",
                "Uống",
                PrescriptionType.PERIODIC,
                com.eldercare.backend.medication.internal.domain.FrequencyType.TIMED_SLOTS,
                "[\"08:00\"]",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                null,
                com.eldercare.backend.medication.api.dto.MedicationSource.FACILITY,
                com.eldercare.backend.medication.internal.domain.PrescriberType.INTERNAL,
                null,
                "Dr. House",
                1L,
                java.time.LocalDate.now(),
                null,
                "Ghi chú",
                PrescriptionStatus.ACTIVE,
                false,
                null,
                null,
                1L,
                Instant.now(),
                Set.of(allergenId),
                Instant.now());
        prescriptionRepository.save(p);
    }

    @Test
    @DisplayName("FR-012: Dị ứng mới phát cảnh báo xung đột cho đơn chứa hoạt chất đó và gọi markAllergyChecked")
    void new_allergy_triggers_alert_and_marks_checked() {
        long allergyId = 5555L;

        allergyListener.onAllergyRecorded(new AllergyRecorded(allergyId, residentId, allergenId));

        // Kiểm tra đã phát cảnh báo
        assertThat(alertCollector.alerts).isNotEmpty();
        MedicationAlertRequestedEvent alert = alertCollector.alerts.get(0);
        assertThat(alert.alertType()).isEqualTo("XUNG_DOT_DI_UNG");
        assertThat(alert.alertLevel()).isEqualTo("TRUNG_BINH");
        assertThat(alert.residentId()).isEqualTo(residentId);

        // Kiểm tra đã gọi markAllergyChecked
        assertThat(testHealthAllergyPort.checkedCalled.get()).isTrue();

        // Kiểm tra tính Idempotent: Gọi lại cùng allergyId không phát thêm cảnh báo
        int alertCount = alertCollector.alerts.size();
        allergyListener.onAllergyRecorded(new AllergyRecorded(allergyId, residentId, allergenId));
        assertThat(alertCollector.alerts.size()).isEqualTo(alertCount);
    }
}
