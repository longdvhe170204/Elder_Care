package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 001 T045 (FR-044, CHK016; research H-02 mục 5): lệnh Chuyển viện qua endpoint của M01 phải nêu căn cứ (sự cố, chỉ định
 * của Bác sĩ hoặc thông tin gia đình, cơ sở y tế); Điều dưỡng, Bác sĩ được làm, Hành chính thì không (Phụ lục 27).
 */
class TransferOutBasisIT extends StatusTestBase {

    private static JsonNode withBasis() {
        var node = JsonNodeFactory.instance.objectNode();
        node.putObject("basis").put("incidentId", 9L);
        return node;
    }

    @Test
    void CHK016_transferOutWithoutBasis_isRefused() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(doctor.user());

        BusinessException e = failure(() -> statusService.transferOut(target(id), reason(), noDetails()));

        assertThat(e).isNotNull();
        assertThat(e.status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(e.errorCode()).isEqualTo("BASIS_REQUIRED");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void CHK016_doctorAndAssignedNurse_canTransferOutWithABasis() {
        long byDoctor = residentIn(ResidentStatus.IN_RESIDENCE);
        long byNurse = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        assignNurse(nurse, byNurse);

        user.set(doctor.user());
        statusService.transferOut(target(byDoctor), reason(), withBasis());
        user.set(nurse.user());
        statusService.transferOut(target(byNurse), reason(), withBasis());

        assertThat(currentStatus(byDoctor)).isEqualTo(ResidentStatus.HOSPITALIZED);
        assertThat(currentStatus(byNurse)).isEqualTo(ResidentStatus.HOSPITALIZED);
    }

    @Test
    void phuLuc27_officeStaffCannotTransferOut() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        assertThatThrownBy(() -> statusService.transferOut(target(id), reason(), withBasis()))
                .isInstanceOf(PermissionDeniedException.class);
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }
}
