package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * FR-043k (Q-425, R21): loại thay đổi "Dịch vụ" chỉ thêm được phục hồi chức năng, hoạt động, đưa đi khám, tiêm; loại khác
 * bị chặn 422 {@code SERVICE_TYPE_NOT_REGISTRABLE} kèm mô tả chỉ đúng luồng cần dùng; bớt được mọi dòng đang có trong hợp
 * đồng; yêu cầu lập trước khi có quy tắc bị chặn lại khi gửi duyệt.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043k_RegistrableServiceTypeIT extends ChangeTestBase {

    private ObjectNode addService(long contractId, long serviceId) {
        return request(
                contractId,
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + serviceId + ", \"action\": \"ADD\"}]}",
                today().plusDays(3));
    }

    private String rejected(long contractId, long serviceId) throws Exception {
        user.set(office.user());
        MvcResult result = createRaw("/api/v1/residency-change-requests", addService(contractId, serviceId))
                .andExpect(status().is(422))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("SERVICE_TYPE_NOT_REGISTRABLE");
        return body;
    }

    @Test
    void R21_addNonRegistrableTypes_rejectedWithFlowHint() throws Exception {
        Setup s = activeSetup();
        Map<String, String> hints = Map.of(
                "CONSUMABLE", "lần dùng",
                "FACILITY_SUPPLIED_MEDICATION", "đơn thuốc",
                "MEAL", "dinh dưỡng",
                "RESIDENCY_FEE", "Giường",
                "CARE", "Mức chăm sóc");
        Map<String, String> names = Map.of(
                "CONSUMABLE", "Tã người lớn",
                "FACILITY_SUPPLIED_MEDICATION", "Paracetamol nguồn viện",
                "MEAL", "Suất ăn",
                "RESIDENCY_FEE", "Phí lưu trú",
                "CARE", "Chăm sóc cơ bản");
        for (var entry : hints.entrySet()) {
            // Mỗi mức chỉ một dịch vụ chăm sóc Hiệu lực: dùng dịch vụ mức Thường xuyên có sẵn, chưa có trong hợp đồng
            long serviceId = "CARE".equals(entry.getKey())
                    ? regularCare
                    : service(entry.getKey(), names.get(entry.getKey()), null, 200_000);
            assertThat(rejected(s.contractId(), serviceId)).as(entry.getKey()).contains(entry.getValue());
        }
    }

    @Test
    void R21_addRegistrableTypes_created() throws Exception {
        Setup s = activeSetup();
        Map<String, String> allowed = Map.of(
                "REHABILITATION", "Phục hồi chức năng",
                "ACTIVITY", "Hoạt động ngoài trời",
                "MEDICAL_ESCORT", "Đưa đi khám",
                "INJECTION", "Tiêm theo y lệnh");
        for (var entry : allowed.entrySet()) {
            long serviceId = service(entry.getKey(), entry.getValue(), null, 200_000);
            assertThat(createChange(addService(s.contractId(), serviceId)).get("approvalRequestId"))
                    .as(entry.getKey())
                    .isNotNull();
        }
    }

    @Test
    void R21_removeExistingConsumableLine_allowed() throws Exception {
        long residentId = residentWithRepresentative();
        long diaper = service("CONSUMABLE", "Tã người lớn", null, 150_000);
        ObjectNode body = body(residentId);
        ObjectNode diaperLine = line(diaper);
        diaperLine.put("inPackage", false);
        ((ArrayNode) body.get("services")).add(line(meal)).add(diaperLine);
        long contractId = activeContract(residentId, body);

        assertThat(createChange(request(
                                contractId,
                                "[\"SERVICE\"]",
                                "{\"services\": [{\"serviceId\": " + diaper + ", \"action\": \"REMOVE\"}]}",
                                today().plusDays(3)))
                        .get("approvalRequestId"))
                .isNotNull();
    }

    @Test
    void R21_requestCreatedBeforeRule_blockedOnSubmit() throws Exception {
        Setup s = activeSetup();
        long serviceId = service("ACTIVITY", "Hoạt động dài hạn", null, 200_000);
        long id = createChange(addService(s.contractId(), serviceId))
                .get("approvalRequestId")
                .asLong();
        // Dịch vụ đổi loại sau khi lập (yêu cầu cũ lập trước khi có quy tắc)
        jdbc.update(
                "UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.type', 'CONSUMABLE') WHERE id = ?",
                serviceId);

        user.set(office.user());
        MvcResult blocked =
                generic(id, "submit", "{}").andExpect(status().isConflict()).andReturn();
        assertThat(blocked.getResponse().getContentAsString()).contains("SERVICE_TYPE_NOT_REGISTRABLE");
    }
}
