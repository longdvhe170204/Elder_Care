package com.cambae.backend.m02;

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
    void BR_M02_16_aScheduleThatCrossesMidnight_is422() throws Exception {
        long residentId = residentWithRepresentative();
        long contractId = dayCareContract(residentId);
        user.set(office.user());

        createRaw(
                        "/api/v1/residency-change-requests",
                        request(
                                contractId,
                                "[\"ARRIVAL_SCHEDULE\"]",
                                "{\"arrivalSchedule\": [{\"dayOfWeek\": 2, \"arrivalTime\": \"20:00\","
                                        + " \"departureTime\": \"08:00\"}]}",
                                today().plusDays(5)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void BR_M02_16_aLineNeedsExactlyOneOfDayOfWeekOrDate_is422() throws Exception {
        long residentId = residentWithRepresentative();
        long contractId = dayCareContract(residentId);
        user.set(office.user());

        createRaw(
                        "/api/v1/residency-change-requests",
                        request(
                                contractId,
                                "[\"ARRIVAL_SCHEDULE\"]",
                                "{\"arrivalSchedule\": [{\"dayOfWeek\": 2, \"date\": \"2031-01-01\","
                                        + " \"arrivalTime\": \"08:00\", \"departureTime\": \"16:00\"}]}",
                                today().plusDays(5)))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void BR_M02_16_aDayCareScheduleChange_isCreatedAndShowsThePreviousSchedule() throws Exception {
        long residentId = residentWithRepresentative();
        long contractId = dayCareContract(residentId);

        var created = createChange(request(contractId, "[\"ARRIVAL_SCHEDULE\"]", SCHEDULE, today().plusDays(5)));

        assertThat(created.get("previousValue")
                        .get("arrivalSchedule")
                        .get(0)
                        .get("dayOfWeek")
                        .asInt())
                .isEqualTo(1);
    }

    @Test
    void BR_M02_16_onlyDayCareContractsHaveAnArrivalSchedule() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(s.contractId(), "[\"ARRIVAL_SCHEDULE\"]", SCHEDULE, today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("NOT_DAY_CARE");
    }

    @Test
    void FR_041_approvalWaitsFor003_whenTheDayCareCapacityCannotBeChecked() throws Exception {
        long residentId = residentWithRepresentative();
        long contractId = dayCareContract(residentId);
        long id = createChange(request(contractId, "[\"ARRIVAL_SCHEDULE\"]", SCHEDULE, today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("DAY_CARE_CAPACITY_CHECK_UNAVAILABLE");
        assertThat(requestStatus(id)).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void FR_041_changeToDayCare_needsAScheduleAndTheCapacityCheck() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(
                        s.contractId(),
                        "[\"RESIDENCE_TYPE\"]",
                        "{\"residenceType\": \"DAY_CARE\"}",
                        today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("ARRIVAL_SCHEDULE_REQUIRED", "DAY_CARE_CAPACITY_CHECK_UNAVAILABLE");
    }

    @Test
    void FR_041_dayCareToInpatient_needsABedAndTheAllocationCheck() throws Exception {
        long residentId = residentWithRepresentative();
        long contractId = dayCareContract(residentId);
        long id = createChange(request(
                        contractId,
                        "[\"RESIDENCE_TYPE\"]",
                        "{\"residenceType\": \"LONG_TERM_RESIDENTIAL\"}",
                        today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("BED_REQUIRED", "BED_ALLOCATION_CHECK_UNAVAILABLE");
    }

    @Test
    void FR_041_aBedChange_waitsFor003() throws Exception {
        Setup s = activeSetup();
        long id = createChange(request(s.contractId(), "[\"BED\"]", "{\"newBedId\": 55}", today().plusDays(5)))
                .get("approvalRequestId")
                .asLong();
        submit(id);

        user.set(manager.user());
        MvcResult blocked = approveRaw(id).andExpect(status().isConflict()).andReturn();

        assertThat(unmetCodes(blocked)).contains("BED_ALLOCATION_CHECK_UNAVAILABLE");
    }
}
