package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.ContractReadApi;
import com.eldercare.backend.admission.api.dto.AttendanceScheduleView;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.spi.ContractApi;
import com.eldercare.backend.roombedallocation.api.spi.AllocationResidencySource;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.IllegalTransactionStateException;

/**
 * 004 T070 (contracts/module-interfaces): phần SPI chưa có test ở {@code BR_M02_09_ContentAtDateIT},
 * {@code AdmissionRealBedIT}: API đọc hợp đồng trả rỗng khi không có dữ liệu; {@code attendanceOn} không có trường tài
 * chính; nguồn lưu trú lấy mức từ lượt đăng ký, mức theo hợp đồng, và lệnh ghi phải chạy trong transaction của 003.
 */
@AutoConfigureMockMvc
@WithMockUser
class ContractSpiEdgeCasesIT extends ContractTestBase {

    @Autowired
    ContractReadApi contractRead;

    @Autowired
    ContractApi contractApi;

    @Autowired
    AllocationResidencySource source;

    @Test
    void residentWithoutContract_readsEmpty_notAnError() {
        long residentId = admissionResident();

        assertThat(contractRead.contentAtDate(residentId, today())).isEmpty();
        assertThat(contractRead.ofResident(residentId)).isEmpty();
        assertThat(contractRead.attendanceOn(residentId, today())).isEmpty();
        assertThat(contractRead.contract(Long.MAX_VALUE)).isEmpty();
        assertThat(contractRead.depositAmount(Long.MAX_VALUE)).isEmpty();
        assertThat(contractApi.currentResidenceType(residentId, today())).isEmpty();
        assertThat(contractApi.residenceTypeHistory(residentId)).isEmpty();
    }

    @Test
    void FR_019_attendanceOn_hasNoFinancialField() {
        assertThat(Arrays.stream(AttendanceScheduleView.class.getRecordComponents())
                        .map(RecordComponent::getName))
                .containsExactlyInAnyOrder("date", "arrivalTime", "departureTime");
    }

    @Test
    void FR_040_careLevelFromRegistration_comesFromTheApplication() throws Exception {
        long residentId = admissionResident();
        assertThat(source.careLevelFromRegistration(residentId)).isEmpty();
        user.set(office.user());

        mvc.perform(post("/api/v1/admission-applications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());

        assertThat(source.careLevelFromRegistration(residentId)).contains(CareLevelCode.BASIC);
        assertThat(source.careLevelFromContract(residentId))
                .as("chưa có hợp đồng")
                .isEmpty();
    }

    @Test
    void FR_040_careLevelFromContract_activeWinsOverADraft() {
        long residentId = admissionResident();
        contracts.contract(
                residentId,
                com.eldercare.backend.admission.api.dto.ContractStatus.DRAFT,
                ResidenceType.LONG_TERM_RESIDENTIAL,
                CareLevel.SPECIAL,
                today());
        contracts.active(residentId, CareLevel.BASIC, today());

        assertThat(source.careLevelFromContract(residentId)).contains(CareLevelCode.BASIC);
    }

    @Test
    void CHK007_writeCallbacks_mustRunInsideTheCallersTransaction() {
        assertThatThrownBy(() -> source.waitingRecordHoldExpired(Long.MAX_VALUE))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> source.allocated(Long.MAX_VALUE, Long.MAX_VALUE))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void Q_395_expectedEnd_isOnlyForShortTerm_startOfTheNextVietnamDay() {
        long residentId = admissionResident();
        contracts.contract(
                residentId,
                com.eldercare.backend.admission.api.dto.ContractStatus.ACTIVE,
                ResidenceType.SHORT_TERM_RESIDENTIAL,
                CareLevel.BASIC,
                today());

        Instant end = source.expectedEnd(residentId).orElseThrow();

        assertThat(end.atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                        .toLocalTime())
                .isEqualTo(java.time.LocalTime.MIDNIGHT);
    }
}
