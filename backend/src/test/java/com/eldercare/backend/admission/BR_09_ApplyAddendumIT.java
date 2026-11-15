package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.api.events.AddendumApplied;
import com.eldercare.backend.resident.api.CareLevelApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T042 (US5 #1, #2; FR-042, FR-044, FR-045, BR-M02-04; quickstart Q11): yêu cầu được duyệt tạo phụ lục Chờ hiệu lực; tới
 * ngày hiệu lực, trong một lần áp dụng, phụ lục thành Đã áp dụng, thêm phiên bản nội dung mới (đơn giá ghi theo ngày hiệu
 * lực), đổi mức chăm sóc hiện hành kèm dịch vụ chăm sóc, đổi ngày kết thúc, phát sự kiện cho 010, 018, 005, 011. Ngày trước
 * ngày hiệu lực vẫn theo nội dung cũ.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_09_ApplyAddendumIT extends ChangeTestBase {

    @Autowired
    CareLevelApi careLevels;

    @Autowired
    com.eldercare.backend.admission.api.ContractReadApi reads;

    private JsonNode contentAt(long contractId, LocalDate date) throws Exception {
        user.set(office.user());
        return JSON.readTree(mvc.perform(get("/api/v1/contracts/" + contractId + "/content?date=" + date))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    void US5_2_onTheEffectiveDate_careLevelChangeAppliesInOneStep() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);
        long requestId = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));

        applyDueOn(effective);

        assertThat(requestStatus(requestId)).isEqualTo("APPLIED");
        JsonNode addendum = addenda(s.contractId()).get(0);
        assertThat(addendum.get("status").asString()).isEqualTo("APPLIED");
        assertThat(versionCount(s.contractId())).isEqualTo(2);
        JsonNode latest = latestContent(s.contractId());
        assertThat(latest.get("careLevel").asString()).isEqualTo("REGULAR");
        long careServices = 0;
        for (JsonNode service : latest.get("services")) {
            if (service.get("serviceId").asLong() == regularCare) {
                careServices++;
                assertThat(service.get("recordedUnitPrice").asLong()).isEqualTo(8_000_000L);
                assertThat(service.get("inPackage").asBoolean()).isTrue();
            }
            assertThat(service.get("serviceId").asLong())
                    .as("dịch vụ chăm sóc mức cũ bị bỏ")
                    .isNotEqualTo(basicCare);
        }
        assertThat(careServices)
                .as("đúng một dịch vụ chăm sóc của mức mới (Q-297)")
                .isEqualTo(1);
        assertThat(careLevels.current(s.residentId())).contains(CareLevel.REGULAR);
        assertThat(jdbc.queryForObject(
                        "SELECT content_version_id FROM contract_version WHERE kind = 'ADDENDUM' AND approval_request_id = ?",
                        Long.class,
                        requestId))
                .isNotNull();
        assertThat(events.stream(AddendumApplied.class))
                .anyMatch(e -> e.contractId() == s.contractId()
                        && e.changeTypes().contains("CARE_LEVEL")
                        && e.effectiveDate().equals(effective));
    }
}
