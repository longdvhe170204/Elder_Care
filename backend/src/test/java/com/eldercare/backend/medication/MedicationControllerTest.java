package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.medication.internal.service.MedicationService;
import com.eldercare.backend.medication.internal.service.MedicationService.CreateMedicationCommand;
import com.eldercare.backend.medication.internal.service.MedicationService.MedicationDetailView;
import com.eldercare.backend.medication.internal.service.MedicationService.UpdateMedicationCommand;
import com.eldercare.backend.medication.web.MedicationController;
import com.eldercare.backend.medication.web.request.MedicationRequest;
import com.eldercare.backend.medication.web.request.ReasonRequest;
import com.eldercare.backend.medication.web.response.MedicationBody;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Kiểm thử đơn vị cho MedicationController.
 */
class MedicationControllerTest {

    private MedicationService medicationService;
    private MedicationController controller;

    @BeforeEach
    void setUp() {
        medicationService = mock(MedicationService.class);
        controller = new MedicationController(medicationService);
    }

    @Test
    @DisplayName("GET /medications: Danh sách thuốc trả về đúng format")
    void list_returnsMedications() {
        MedicationDetailView view = new MedicationDetailView(
                1L, "Paracetamol", "500mg", "Viên nén", "Uống", "viên", List.of(101L), 30, 30, false, true, false, 1);

        when(medicationService.list(eq("para"), eq(true))).thenReturn(List.of(view));

        List<MedicationBody> responses = controller.list("para", true);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).name()).isEqualTo("Paracetamol");
        assertThat(responses.get(0).active()).isTrue();
    }

    @Test
    @DisplayName("GET /medications/{id}: Trả về thuốc và ETag phiên bản")
    void getById_returnsDetailAndEtag() {
        MedicationDetailView view = new MedicationDetailView(
                1L, "Amlodipin", "5mg", "Viên nang", "Uống", "viên", List.of(201L), null, null, false, true, true, 2);

        when(medicationService.getById(1L)).thenReturn(view);

        ResponseEntity<MedicationBody> response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"2\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Amlodipin");
        assertThat(response.getBody().missingPricingItem()).isTrue();
    }

    @Test
    @DisplayName("POST /medications: Thêm thuốc mới trả về 201 CREATED")
    void create_returns201() {
        MedicationRequest body = new MedicationRequest(
                null, "Paracetamol", "500mg", "Viên nén", "Uống", "viên", List.of(101L), 30, 30, false, null);

        MedicationDetailView created = new MedicationDetailView(
                10L, "Paracetamol", "500mg", "Viên nén", "Uống", "viên", List.of(101L), 30, 30, false, true, false, 0);

        when(medicationService.create(any(CreateMedicationCommand.class))).thenReturn(created);

        ResponseEntity<Void> response = controller.create(null, body);

        // api-006: 201 không thân, phiên bản ở ETag
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"0\"");
        assertThat(response.getBody()).isNull();
    }

    @Test
    @DisplayName("PUT /medications/{id}: Sửa thuốc trả về 200 OK")
    void update_returns200() {
        MedicationRequest body = new MedicationRequest(
                10L, "Paracetamol Extra", "500mg", "Viên nén", "Uống", "viên", List.of(101L), 45, 45, false, true);

        MedicationDetailView updated = new MedicationDetailView(
                10L,
                "Paracetamol Extra",
                "500mg",
                "Viên nén",
                "Uống",
                "viên",
                List.of(101L),
                45,
                45,
                false,
                true,
                false,
                1);

        when(medicationService.update(eq(10L), eq("\"0\""), any(UpdateMedicationCommand.class)))
                .thenReturn(updated);

        ResponseEntity<Void> response = controller.update(10L, "\"0\"", null, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
    }

    @Test
    @DisplayName("POST /medications/{id}/deactivate: Ngừng hiệu lực thuốc")
    void deactivate_returns200() {
        MedicationDetailView deactivated = new MedicationDetailView(
                10L, "Paracetamol", "500mg", "Viên nén", "Uống", "viên", List.of(101L), 30, 30, false, false, false, 1);

        when(medicationService.deactivate(eq(10L), any(Reason.class))).thenReturn(deactivated);

        ResponseEntity<Void> response = controller.deactivate(10L, null, new ReasonRequest("Hết sản xuất"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
    }
}
