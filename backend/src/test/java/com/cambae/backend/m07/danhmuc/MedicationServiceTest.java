package com.cambae.backend.m07.danhmuc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.m07.internal.domain.Medication;
import com.cambae.backend.m07.internal.domain.MedicationStatus;
import com.cambae.backend.m07.internal.persistence.MedicationRepository;
import com.cambae.backend.m07.internal.service.MedicationCatalogPort;
import com.cambae.backend.m07.internal.service.MedicationService;
import com.cambae.backend.m07.internal.service.MedicationService.CreateMedicationCommand;
import com.cambae.backend.m07.internal.service.MedicationService.MedicationDetailView;
import com.cambae.backend.m07.internal.service.MedicationService.UpdateMedicationCommand;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử đơn vị cho nghiệp vụ Danh mục thuốc (FR-001, FR-002, FR-003, AC-01).
 */
class MedicationServiceTest {

    private MedicationRepository repository;
    private MedicationCatalogPort catalogPort;
    private MedicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(MedicationRepository.class);
        catalogPort = mock(MedicationCatalogPort.class);
        service = new MedicationService(repository, catalogPort);
    }

    private void setId(Medication med, Long id) {
        try {
            Field idField = Medication.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(med, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("FR-001: Tạo thuốc mới thành công và cờ chưa có mục vật phẩm đúng")
    void create_valid_success() {
        CreateMedicationCommand cmd = new CreateMedicationCommand(
                "Paracetamol",
                "500mg",
                "Viên nén",
                "Uống",
                "viên",
                Set.of(101L),
                30,
                30,
                false);

        when(repository.existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCase(
                "Paracetamol", "500mg", "Viên nén")).thenReturn(false);

        when(repository.save(any(Medication.class))).thenAnswer(invocation -> {
            Medication m = invocation.getArgument(0);
            setId(m, 1L);
            return m;
        });

        when(catalogPort.hasHospitalProcurementItem(1L)).thenReturn(false);

        MedicationDetailView result = service.create(cmd);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Paracetamol");
        assertThat(result.concentration()).isEqualTo("500mg");
        assertThat(result.dosageForm()).isEqualTo("Viên nén");
        assertThat(result.allergenIds()).containsExactly(101L);
        assertThat(result.active()).isTrue();
        assertThat(result.missingPricingItem()).isTrue(); // chưa có mục vật phẩm ở 004
    }

    @Test
    @DisplayName("FR-001: Trùng tên, hàm lượng, dạng bào chế thì báo lỗi 409 TRUNG_THUOC")
    void create_duplicateIdentity_throwsConflict() {
        CreateMedicationCommand cmd = new CreateMedicationCommand(
                "Paracetamol",
                "500mg",
                "Viên nén",
                "Uống",
                "viên",
                Set.of(101L),
                null,
                null,
                false);

        when(repository.existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCase(
                "Paracetamol", "500mg", "Viên nén")).thenReturn(true);

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(ConditionNotMetException.class)
                .hasMessageContaining("Điều kiện chưa thỏa");
    }

    @Test
    @DisplayName("FR-001: Thiếu hoạt chất dị nguyên ném lỗi")
    void create_missingAllergen_throwsException() {
        CreateMedicationCommand cmd = new CreateMedicationCommand(
                "Paracetamol",
                "500mg",
                "Viên nén",
                "Uống",
                "viên",
                Set.of(),
                null,
                null,
                false);

        assertThatThrownBy(() -> service.create(cmd))
                .isInstanceOf(ConditionNotMetException.class);
    }

    @Test
    @DisplayName("FR-002: Cập nhật thuốc thành công khi đúng phiên bản If-Match")
    void update_validIfMatch_success() {
        Medication med = new Medication(
                "Paracetamol", "500mg", "Viên nén", "Uống", "viên", 30, 30, false, Set.of(101L));
        setId(med, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(med));
        when(repository.existsByNameIgnoreCaseAndConcentrationIgnoreCaseAndDosageFormIgnoreCaseAndIdNot(
                eq("Paracetamol Extra"), eq("500mg"), eq("Viên nén"), eq(1L))).thenReturn(false);

        UpdateMedicationCommand cmd = new UpdateMedicationCommand(
                "Paracetamol Extra", "500mg", "Viên nén", "Uống", "viên", Set.of(101L, 102L), 45, 45, false);

        MedicationDetailView updated = service.update(1L, "\"0\"", cmd);

        assertThat(updated.name()).isEqualTo("Paracetamol Extra");
        assertThat(updated.allergenIds()).containsExactlyInAnyOrder(101L, 102L);
        assertThat(updated.beforeWindowMinutes()).isEqualTo(45);
    }

    @Test
    @DisplayName("FR-002: Sai phiên bản If-Match ném VersionMismatchException (412)")
    void update_wrongIfMatch_throwsVersionMismatch() {
        Medication med = new Medication(
                "Paracetamol", "500mg", "Viên nén", "Uống", "viên", 30, 30, false, Set.of(101L));
        setId(med, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(med));

        UpdateMedicationCommand cmd = new UpdateMedicationCommand(
                "Paracetamol Extra", "500mg", "Viên nén", "Uống", "viên", Set.of(101L), 30, 30, false);

        assertThatThrownBy(() -> service.update(1L, "\"99\"", cmd))
                .isInstanceOf(VersionMismatchException.class);
    }

    @Test
    @DisplayName("FR-002: Ngừng hiệu lực chuyển trạng thái sang INACTIVE")
    void deactivate_success() {
        Medication med = new Medication(
                "Paracetamol", "500mg", "Viên nén", "Uống", "viên", 30, 30, false, Set.of(101L));
        setId(med, 1L);

        when(repository.findById(1L)).thenReturn(Optional.of(med));

        MedicationDetailView deactivated = service.deactivate(1L, Reason.of("Nhà sản xuất thu hồi"));

        assertThat(deactivated.active()).isFalse();
        assertThat(med.getStatus()).isEqualTo(MedicationStatus.INACTIVE);
    }

    @Test
    @DisplayName("Tra cứu thuốc không tồn tại ném NotFoundException (404)")
    void getById_notFound_throwsNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(NotFoundException.class);
    }
}
