package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.support.fake.m02.FakeFinalPeriods;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T057, T060 (US7; FR-065, FR-046; research T-06): lệnh Kết thúc lưu trú đóng yêu cầu thay đổi lưu trú đã duyệt nhưng chưa
 * tới ngày hiệu lực bằng {@code closeForFinalState} của 000 (Đã duyệt thành Áp dụng không thành), phụ lục Chờ hiệu lực thành
 * Không áp dụng và thuộc tính được nhả.
 */
@AutoConfigureMockMvc
@WithMockUser
class EndingClosesRequestsIT extends ChangeTestBase {

    @Autowired
    FakeFinalPeriods finalPeriods;

    @BeforeEach
    void setUp() {
        finalPeriods.reset();
        otherModules.interestedIn(
                StatusCommand.COMPLETE_ADMISSION, StatusCommand.CANCEL_ADMISSION, StatusCommand.END_RESIDENCY);
    }

    @Test
    void FR_065_approvedRequestWaitingForItsDate_isClosedAsApplyFailed_andTheAddendumIsNotApplied() throws Exception {
        Setup s = activeSetup();
        setStatus(s.residentId(), ResidentStatus.IN_RESIDENCE.name());
        LocalDate today = today();
        long requestId = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today.plusDays(10)));
        assertThat(addenda(s.contractId()).get(0).get("status").asString()).isEqualTo("PENDING_EFFECTIVE");
        long endingId = createEnding(s.residentId(), today);
        user.set(office.user());
        mvc.perform(post("/api/v1/residency-endings/" + endingId + "/handover")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipientType\":\"RECEIVING_FACILITY\",\"facilityName\":\"Bệnh viện\","
                                + "\"handoverAt\":\"" + time.now() + "\",\"handoverStaffId\":" + office.staffId()
                                + "}"))
                .andExpect(status().isCreated());
        noonOf(today);
        ObjectNode details = JSON.createObjectNode();
        details.put("residencyEndingId", endingId);
        details.put(
                "effectiveAt",
                today.atTime(12, 0)
                        .atZone(com.cambae.backend.common.time.BusinessTime.ZONE)
                        .toInstant()
                        .toString());

        statusService.endResidency(
                ResidentStatusService.target(s.residentId()), new Reason("Kết thúc lưu trú"), details);

        assertThat(requestStatus(requestId)).isEqualTo("APPLY_FAILED");
        JsonNode addendum = addenda(s.contractId()).get(0);
        assertThat(addendum.get("status").asString()).isEqualTo("NOT_APPLICABLE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM change_request_attribute WHERE approval_request_id = ? AND pending_effective",
                        Integer.class,
                        requestId))
                .as("thuộc tính được nhả")
                .isZero();
        assertThat(contractStatus(s.contractId())).isEqualTo("TERMINATED");
        assertThat(versionCount(s.contractId()))
                .as("không thêm phiên bản nội dung")
                .isEqualTo(1);
    }

    private long createEnding(long residentId, LocalDate expectedEndDate) throws Exception {
        user.set(office.user());
        String body = "{\"residentId\":%d,\"endCase\":\"CONTRACT_TERMINATION\",\"expectedEndDate\":\"%s\","
                        .formatted(residentId, expectedEndDate)
                + "\"requestedBy\":\"Con trai\",\"reason\":\"Gia đình chấm dứt hợp đồng\"}";
        return JSON.readTree(mvc.perform(post("/api/v1/residency-endings")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get("id")
                .asLong();
    }
}
