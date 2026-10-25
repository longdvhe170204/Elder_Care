package com.cambae.backend.m07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.service.AllergyItemView;
import com.cambae.backend.m07.internal.service.HealthAllergyPort;
import com.cambae.backend.m07.internal.service.PrescriptionService;
import com.cambae.backend.m07.internal.service.ReconciliationCheckPort;
import com.cambae.backend.m07.web.request.ConfirmedIssueBody;
import com.cambae.backend.m07.web.request.CreatePrescriptionBody;
import com.cambae.backend.m07.web.request.FrequencyBody;
import com.cambae.backend.m07.web.request.PrescriberBody;
import com.cambae.backend.m07.web.response.PrescriptionResponse;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Kiểm thử tích hợp BR-M07-05: Quyền kê đơn và điều kiện tính hợp lệ của đơn thuốc.
 */
class BR_M07_05_PrescriberAuthorityIT extends MySqlIntegrationTestBase {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private Long residentId = 8801L;
    private Long medId = 8801L;

    @BeforeEach
    void setUp() {
        jdbc.update(
                "INSERT INTO resident_medication (resident_id, reconciliation_due_at, reconciliation_reason, reconciliation_warned_at, version) "
                        + "VALUES (?, NULL, NULL, NULL, 0) ON DUPLICATE KEY UPDATE version = version",
                residentId);

        if (!medicationRepository.existsById(medId)) {
            Medication med = new Medication(
                    "Amoxicillin 500mg", "500mg", "Capsule", "Uống", "Viên", 30, 30, false, Set.of(101L));
            med = medicationRepository.save(med);
            medId = med.getId();
        }
    }

    @TestConfiguration
    static class FakePortConfig {
        @Bean
        @Primary
        HealthAllergyPort healthAllergyPort() {
            return new HealthAllergyPort() {
                @Override
                public List<AllergyItemView> getActiveAllergies(long residentId) {
                    return List.of(new AllergyItemView(101L, 101L, "Penicillin", "NANG", false));
                }

                @Override
                public void markAllergyChecked(long allergyId, String module) {}
            };
        }

        @Bean
        @Primary
        ReconciliationCheckPort reconciliationCheckPort() {
            return resId -> false;
        }
    }

    @Test
    @DisplayName("BR-M07-05: Đơn nội bộ thiếu chứng chỉ hành nghề bác sĩ bị từ chối với 403 LEGAL_CONDITION_NOT_MET")
    void internal_prescription_without_doctor_license_throws_403() {
        CreatePrescriptionBody body = new CreatePrescriptionBody();
        body.setMedicationId(medId);
        body.setDosage(new BigDecimal("1.0"));
        body.setRoute("Uống");
        body.setPrescriptionType("REGULAR");
        body.setMedicationSource("FACILITY");
        body.setStartedAt(Instant.now());

        FrequencyBody freq = new FrequencyBody();
        freq.setType("TIMED_SLOTS");
        freq.setTimedSlots(List.of("08:00", "20:00"));
        body.setFrequency(freq);

        PrescriberBody prescriber = new PrescriberBody();
        prescriber.setType("INTERNAL");
        prescriber.setPrescriberId(2L);
        body.setPrescriber(prescriber);

        // Kê bởi người không có chứng chỉ bác sĩ (isDoctorWithLicense = false)
        assertThatThrownBy(() -> prescriptionService.create(residentId, body, 2L, false))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.errorCode()).isEqualTo("LEGAL_CONDITION_NOT_MET");
                });
    }

    @Test
    @DisplayName("BR-M07-05: Đơn bên ngoài thiếu thông tin cơ sở/người kê/ngày kê bị từ chối với 422 INVALID_VALUE")
    void external_prescription_missing_facility_info_throws_422() {
        CreatePrescriptionBody body = new CreatePrescriptionBody();
        body.setMedicationId(medId);
        body.setDosage(new BigDecimal("1.0"));
        body.setRoute("Uống");
        body.setPrescriptionType("REGULAR");
        body.setMedicationSource("FACILITY");
        body.setStartedAt(Instant.now());

        FrequencyBody freq = new FrequencyBody();
        freq.setType("TIMED_SLOTS");
        freq.setTimedSlots(List.of("08:00"));
        body.setFrequency(freq);

        PrescriberBody prescriber = new PrescriberBody();
        prescriber.setType("EXTERNAL");
        // Thiếu tenCoSo, nguoiKe, ngayKe
        body.setPrescriber(prescriber);

        assertThatThrownBy(() -> prescriptionService.create(residentId, body, 1L, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.errorCode()).isEqualTo("INVALID_VALUE");
                });
    }

    @Test
    @DisplayName(
            "FR-007, BR-M07-06: Phát hiện xung đột dị ứng mà chưa xác nhận lý do thì bị chặn với 409 CAN_XAC_NHAN_VAN_DE")
    void allergy_conflict_without_confirmation_throws_409() {
        CreatePrescriptionBody body = new CreatePrescriptionBody();
        body.setMedicationId(medId); // chứa hoạt chất 101L Penicillin, xung đột với dị ứng
        body.setDosage(new BigDecimal("1.0"));
        body.setRoute("Uống");
        body.setPrescriptionType("REGULAR");
        body.setMedicationSource("FACILITY");
        body.setStartedAt(Instant.now());

        FrequencyBody freq = new FrequencyBody();
        freq.setType("TIMED_SLOTS");
        freq.setTimedSlots(List.of("08:00"));
        body.setFrequency(freq);

        PrescriberBody prescriber = new PrescriberBody();
        prescriber.setType("INTERNAL");
        prescriber.setPrescriberId(1L);
        body.setPrescriber(prescriber);

        assertThatThrownBy(() -> prescriptionService.create(residentId, body, 1L, true))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.errorCode()).isEqualTo("CAN_XAC_NHAN_VAN_DE");
                    assertThat(be.properties()).containsKey("vanDe");
                });
    }

    @Test
    @DisplayName("FR-007: Khi đã gửi kèm xác nhận lý do vấn đề thì tạo đơn thành công")
    void allergy_conflict_with_confirmation_succeeds() {
        CreatePrescriptionBody body = new CreatePrescriptionBody();
        body.setMedicationId(medId);
        body.setDosage(new BigDecimal("1.0"));
        body.setRoute("Uống");
        body.setPrescriptionType("REGULAR");
        body.setMedicationSource("FACILITY");
        body.setStartedAt(Instant.now());

        FrequencyBody freq = new FrequencyBody();
        freq.setType("TIMED_SLOTS");
        freq.setTimedSlots(List.of("08:00"));
        body.setFrequency(freq);

        PrescriberBody prescriber = new PrescriberBody();
        prescriber.setType("INTERNAL");
        prescriber.setPrescriberId(1L);
        body.setPrescriber(prescriber);

        // Đã có xác nhận lý do
        body.setConfirmedIssues(
                List.of(new ConfirmedIssueBody("ALLERGY:101", "Bác sĩ đã cân nhắc lợi ích vượt trội nguy cơ")));

        PrescriptionResponse resp = prescriptionService.create(residentId, body, 1L, true);
        assertThat(resp).isNotNull();
        assertThat(resp.id()).isNotNull();
        assertThat(resp.confirmedIssues()).isNotEmpty();
    }
}
