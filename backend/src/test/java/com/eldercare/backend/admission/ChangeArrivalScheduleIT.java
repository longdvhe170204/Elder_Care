package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T043 (BR-M02-16, FR-037, FR-041, Q-286): yêu cầu đổi lịch đến của người bán trú. Lịch đến bán trú không qua đêm; loại
 * này chỉ dành cho hợp đồng bán trú. Kiểm sức chứa khu nghỉ khi duyệt cần {@code SucChuaBanTruApi} của 003 (chưa công bố,
 * team-plan mục 8), nên tới lúc đó việc duyệt bị chặn bằng điều kiện nêu rõ thay vì cho qua im lặng.
 */
@AutoConfigureMockMvc
@WithMockUser
class ChangeArrivalScheduleIT extends ChangeTestBase {

    private static final String SCHEDULE =
            "{\"arrivalSchedule\": [{\"dayOfWeek\": 2, \"arrivalTime\": \"08:00\", \"departureTime\": \"16:00\"}]}";

    private long dayCareContract(long residentId) throws Exception {
        ObjectNode body = body(residentId);
        body.put("residenceType", "DAY_CARE");
        body.remove("roomTypeId");
        body.put("residencyFeeBillingPeriod", "SESSION");
        ArrayNode schedule = body.putArray("arrivalSchedule");
        ObjectNode line = schedule.addObject();
        line.put("dayOfWeek", 1);
        line.put("arrivalTime", "08:00");
        line.put("departureTime", "16:00");
        return activeContract(residentId, body);
    }

    @Test
    void FR_041_aBedChange_waitsFor003() throws Exception {
        Setup s = activeSetup();
        cancelFixtureAllocation(s.residentId());
        long id = createChange(request(s.contractId(), "[\"BED\"]", "{\"newBedId\": 55}", today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        // 003 T033 (2026-11-06): 003 đã có bản cài thật, kiểm FR-018 của giường mới; người chưa có phân bổ đang
        // hiệu lực thì chặn với mã của 003 (trước đó là BED_ALLOCATION_CHECK_UNAVAILABLE khi chưa có bản cài).
        assertThat(unmetCodes(blocked)).contains("ALLOCATION_NOT_ACTIVE");
    }

    @Test
    void BR_M02_16_unchangedOrDuplicateArrivalSchedule_rejectedOnCreate() throws Exception {
        long residentId = residentWithRepresentative();
        long contractId = dayCareContract(residentId);

        user.set(office.user());
        // Trùng y hệt lịch hiện tại (dayOfWeek=1, 08:00-16:00) -> 422 ARRIVAL_SCHEDULE_UNCHANGED
        MvcResult unchanged = createRaw(
                        "/api/v1/residency-change-requests",
                        request(
                                contractId,
                                "[\"ARRIVAL_SCHEDULE\"]",
                                "{\"arrivalSchedule\": [{\"dayOfWeek\": 1, \"arrivalTime\": \"08:00\", \"departureTime\": \"16:00\"}]}",
                                today().plusDays(2)))
                .andExpect(status().is(422))
                .andReturn();
        assertThat(unchanged.getResponse().getContentAsString()).contains("ARRIVAL_SCHEDULE_UNCHANGED");

        // Trùng thứ trong lịch mới -> 422 ARRIVAL_SCHEDULE_DUPLICATE_DAY
        MvcResult duplicate = createRaw(
                        "/api/v1/residency-change-requests",
                        request(
                                contractId,
                                "[\"ARRIVAL_SCHEDULE\"]",
                                "{\"arrivalSchedule\": [{\"dayOfWeek\": 2, \"arrivalTime\": \"08:00\", \"departureTime\": \"12:00\"}, {\"dayOfWeek\": 2, \"arrivalTime\": \"13:00\", \"departureTime\": \"16:00\"}]}",
                                today().plusDays(2)))
                .andExpect(status().is(422))
                .andReturn();
        assertThat(duplicate.getResponse().getContentAsString()).contains("ARRIVAL_SCHEDULE_DUPLICATE_DAY");

        // Đổi sang thứ khác hợp lệ -> 201
        assertThat(createChange(request(contractId, "[\"ARRIVAL_SCHEDULE\"]", SCHEDULE, today().plusDays(2)))
                        .get("approvalRequestId"))
                .isNotNull();
    }
}
