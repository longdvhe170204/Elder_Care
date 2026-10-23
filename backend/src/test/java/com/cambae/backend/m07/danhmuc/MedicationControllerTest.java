package com.cambae.backend.m07.danhmuc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m07.internal.service.MedicationService;
import com.cambae.backend.m07.internal.service.MedicationService.CreateMedicationCommand;
import com.cambae.backend.m07.internal.service.MedicationService.MedicationDetailView;
import com.cambae.backend.m07.internal.service.MedicationService.UpdateMedicationCommand;
import com.cambae.backend.m07.web.MedicationController;
import com.cambae.backend.m07.web.request.MedicationBody;
import com.cambae.backend.m07.web.request.ReasonBody;
import com.cambae.backend.m07.web.response.MedicationResponse;
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
    @DisplayName("GET /thuoc: Danh sách thuốc trả về đúng format")
    void list_returnsMedications() {
        MedicationDetailView view = new MedicationDetailView(
                1L,
                "Paracetamol",
                "500mg",
                "Viên nén",
                "Uống",
                "viên",
                List.of(101L),
                30,
                30,
                false,
                true,
                false,
                1);

        when(medicationService.list(eq("para"), eq(true))).thenReturn(List.of(view));

        List<MedicationResponse> responses = controller.list("para", true);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).ten()).isEqualTo("Paracetamol");
        assertThat(responses.get(0).hieuLuc()).isTrue();
    }

    @Test
    @DisplayName("GET /thuoc/{id}: Trả về thuốc và ETag phiên bản")
    void getById_returnsDetailAndEtag() {
        MedicationDetailView view = new MedicationDetailView(
                1L,
                "Amlodipin",
                "5mg",
                "Viên nang",
                "Uống",
                "viên",
                List.of(201L),
                null,
                null,
                false,
                true,
                true,
                2);

        when(medicationService.getById(1L)).thenReturn(view);

        ResponseEntity<MedicationResponse> response = controller.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"2\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().ten()).isEqualTo("Amlodipin");
        assertThat(response.getBody().chuaCoMucVatPham()).isTrue();
    }

    @Test
    @DisplayName("POST /thuoc: Thêm thuốc mới trả về 201 CREATED")
    void create_returns201() {
        MedicationBody body = new MedicationBody(
                "Paracetamol", "500mg", "Viên nén", "Uống", "viên", List.of(101L), 30, 30, false);

        MedicationDetailView created = new MedicationDetailView(
                10L,
                "Paracetamol",
                "500mg",
                "Viên nén",
                "Uống",
                "viên",
                List.of(101L),
                30,
                30,
                false,
                true,
                false,
                0);

        when(medicationService.create(any(CreateMedicationCommand.class))).thenReturn(created);

        ResponseEntity<MedicationResponse> response = controller.create(null, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"0\"");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("PUT /thuoc/{id}: Sửa thuốc trả về 200 OK")
    void update_returns200() {
        MedicationBody body = new MedicationBody(
                "Paracetamol Extra", "500mg", "Viên nén", "Uống", "viên", List.of(101L), 45, 45, false);

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

        when(medicationService.update(eq(10L), eq("\"0\""), any(UpdateMedicationCommand.class))).thenReturn(updated);

        ResponseEntity<MedicationResponse> response = controller.update(10L, "\"0\"", null, body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
        assertThat(response.getBody().ten()).isEqualTo("Paracetamol Extra");
    }

    @Test
    @DisplayName("POST /thuoc/{id}/ngung-hieu-luc: Ngừng hiệu lực thuốc")
    void deactivate_returns200() {
        MedicationDetailView deactivated = new MedicationDetailView(
                10L,
                "Paracetamol",
                "500mg",
                "Viên nén",
                "Uống",
                "viên",
                List.of(101L),
                30,
                30,
                false,
                false,
                false,
                1);

        when(medicationService.deactivate(eq(10L), any(Reason.class))).thenReturn(deactivated);

        ResponseEntity<MedicationResponse> response =
                controller.deactivate(10L, null, new ReasonBody("Hết sản xuất"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().hieuLuc()).isFalse();
    }
}
