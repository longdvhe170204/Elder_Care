package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.api.dto.MedicationSource;
import com.eldercare.backend.medication.internal.domain.PrescriberType;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionIssueType;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import com.eldercare.backend.medication.internal.service.AllergyItemView;
import com.eldercare.backend.medication.internal.service.PrescriptionCheckResult;
import com.eldercare.backend.medication.internal.service.PrescriptionIssueView;
import com.eldercare.backend.medication.internal.service.PrescriptionValidator;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T010: Test đơn vị kiểm tra trùng hoạt chất và xung đột dị ứng (FR-007, FR-008, BR-M07-06).
 */
class BR_M07_06_DuplicateIngredientAllergyTest {

    private PrescriptionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PrescriptionValidator();
    }

    private Prescription createPrescription(Long id, Set<Long> allergenIds, PrescriptionStatus status) {
        Prescription prescription = new Prescription(
                1L,
                100L,
                new BigDecimal("5.00"),
                "pill",
                "Oral",
                PrescriptionType.PERIODIC,
                null,
                "[\"08:00\"]",
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                null,
                MedicationSource.FACILITY,
                PrescriberType.INTERNAL,
                null,
                null,
                1L,
                null,
                null,
                null,
                status,
                false,
                null,
                null,
                1L,
                Instant.now(),
                allergenIds,
                Instant.now());
        if (id != null) {
            try {
                Field f = Prescription.class.getDeclaredField("id");
                f.setAccessible(true);
                f.set(prescription, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return prescription;
    }

    @Test
    @DisplayName(
            "Independent Test: Trùng hoạt chất - Đơn đang có Amlodipin (1001), thêm thuốc chứa Amlodipin -> Cảnh báo DUPLICATE_ACTIVE_INGREDIENT")
    void validate_duplicateActiveIngredient_warnsIssue() {
        Prescription amlodipinePrescription = createPrescription(10L, Set.of(1001L), PrescriptionStatus.ACTIVE);
        List<Prescription> existing = List.of(amlodipinePrescription);
        List<AllergyItemView> allergies = List.of();

        // Thuốc phối hợp Amlodipin (1001) + Valsartan (1002)
        Set<Long> newAllergenIds = Set.of(1001L, 1002L);

        PrescriptionCheckResult result = validator.validate(newAllergenIds, existing, allergies, null);

        assertThat(result.issues()).hasSize(1);
        PrescriptionIssueView issue = result.issues().get(0);
        assertThat(issue.issueType()).isEqualTo(PrescriptionIssueType.DUPLICATE_ACTIVE_INGREDIENT);
        assertThat(issue.duplicatePrescriptionId()).isEqualTo(10L);
        assertThat(issue.issueCode()).isEqualTo("DUPLICATE_INGREDIENT_10_1001");
        assertThat(result.reviewItems()).isEmpty();

        // Chưa có lý do xác nhận -> hasUnconfirmedIssues là true
        assertThat(result.hasUnconfirmedIssues(Map.of())).isTrue();

        // Cung cấp lý do -> false (được phép tiếp tục)
        assertThat(result.hasUnconfirmedIssues(
                        Map.of("DUPLICATE_INGREDIENT_10_1001", "Bác sĩ chuyên khoa chỉ định phối hợp")))
                .isFalse();
    }

    @Test
    @DisplayName(
            "Independent Test: Xung đột dị ứng - Hồ sơ có dị ứng Penicillin (2001), đơn mới là Amoxicillin (2001) -> Cảnh báo ALLERGY_CONFLICT")
    void validate_allergyConflict_warnsIssue() {
        List<Prescription> existing = List.of();
        AllergyItemView penicillinAllergy = AllergyItemView.fromCatalog(50L, 2001L, "Penicillin", "NANG");
        List<AllergyItemView> allergies = List.of(penicillinAllergy);

        Set<Long> amoxicillinAllergens = Set.of(2001L);

        PrescriptionCheckResult result = validator.validate(amoxicillinAllergens, existing, allergies, null);

        assertThat(result.issues()).hasSize(1);
        PrescriptionIssueView issue = result.issues().get(0);
        assertThat(issue.issueType()).isEqualTo(PrescriptionIssueType.ALLERGY_CONFLICT);
        assertThat(issue.allergyItemId()).isEqualTo(50L);
        assertThat(issue.activeIngredient()).isEqualTo("Penicillin");
        assertThat(issue.severity()).isEqualTo("NANG");
        assertThat(issue.issueCode()).isEqualTo("ALLERGY_50");
        assertThat(result.reviewItems()).isEmpty();
    }

    @Test
    @DisplayName(
            "Independent Test: Dị ứng loại 'khác' (ghi tự do) -> Đưa vào reviewItems, không chặn việc lưu đơn (FR-008)")
    void validate_otherAllergyReview_doesNotBlock() {
        List<Prescription> existing = List.of();
        AllergyItemView freeTextAllergy =
                AllergyItemView.fromFreeText(51L, "Dị ứng mẩn ngứa khi dùng kháng sinh lạ", "TRUNG_BINH");
        List<AllergyItemView> allergies = List.of(freeTextAllergy);

        Set<Long> newAllergenIds = Set.of(3001L);

        PrescriptionCheckResult result = validator.validate(newAllergenIds, existing, allergies, null);

        // Không có vấn đề chặn
        assertThat(result.issues()).isEmpty();
        assertThat(result.hasUnconfirmedIssues(Map.of())).isFalse();

        // Có trong reviewItems để người nhập xem xét
        assertThat(result.reviewItems()).hasSize(1);
        PrescriptionIssueView review = result.reviewItems().get(0);
        assertThat(review.issueType()).isEqualTo(PrescriptionIssueType.OTHER_ALLERGY_REVIEW);
        assertThat(review.allergyItemId()).isEqualTo(51L);
        assertThat(review.manualReviewRequired()).isTrue();
    }

    @Test
    @DisplayName("Independent Test: Đơn hợp lệ (Metformin) -> Không trùng hoạt chất, không xung đột dị ứng -> Lưu ngay")
    void validate_validPrescription_noIssues() {
        Prescription amlodipinePrescription = createPrescription(10L, Set.of(1001L), PrescriptionStatus.ACTIVE);
        List<Prescription> existing = List.of(amlodipinePrescription);
        AllergyItemView penicillinAllergy = AllergyItemView.fromCatalog(50L, 2001L, "Penicillin", "NANG");
        List<AllergyItemView> allergies = List.of(penicillinAllergy);

        // Metformin hoạt chất 4001
        Set<Long> metforminAllergens = Set.of(4001L);

        PrescriptionCheckResult result = validator.validate(metforminAllergens, existing, allergies, null);

        assertThat(result.issues()).isEmpty();
        assertThat(result.reviewItems()).isEmpty();
        assertThat(result.hasUnconfirmedIssues(Map.of())).isFalse();
    }

    @Test
    @DisplayName("BR-M07-06: Đổi liều - Bỏ qua chính đơn bị thay thế, không báo trùng hoạt chất với chính nó")
    void validate_doseChange_ignoresReplacedPrescription() {
        Prescription oldPrescription = createPrescription(10L, Set.of(1001L), PrescriptionStatus.ACTIVE);
        List<Prescription> existing = List.of(oldPrescription);

        // Đơn thay thế chứa cùng hoạt chất 1001L
        Set<Long> replacementAllergens = Set.of(1001L);

        // Truyền replacedPrescriptionId = 10L
        PrescriptionCheckResult result = validator.validate(replacementAllergens, existing, List.of(), 10L);

        assertThat(result.issues()).isEmpty();
    }

    @Test
    @DisplayName("BR-M07-06: Đơn đã ngừng hoặc hết hạn không bị tính trùng hoạt chất")
    void validate_terminatedPrescription_notConsidered() {
        Prescription discontinued = createPrescription(11L, Set.of(1001L), PrescriptionStatus.DISCONTINUED);
        Prescription expired = createPrescription(12L, Set.of(1001L), PrescriptionStatus.EXPIRED);
        List<Prescription> existing = List.of(discontinued, expired);

        Set<Long> newAllergens = Set.of(1001L);

        PrescriptionCheckResult result = validator.validate(newAllergens, existing, List.of(), null);

        assertThat(result.issues()).isEmpty();
    }
}
