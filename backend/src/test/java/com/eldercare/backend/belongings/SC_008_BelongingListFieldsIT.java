package com.eldercare.backend.belongings;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * 013 T032 (SC-008, FR-011): mọi đồ gửi chưa ở trạng thái cuối có đủ trạng thái, người giữ (trừ Thất lạc), vị trí (khi
 * Đang giữ hoặc Hư hỏng) và thời điểm bản ghi bàn giao gần nhất, tra được cùng lúc ở một lần gọi danh sách
 * {@code GET /residents/{residentId}/belongings}, không cần mở từng bản ghi bàn giao.
 */
class SC_008_BelongingListFieldsIT extends HandoverTestBase {

    @Test
    void SC_008_everyNonFinalBelonging_hasStatusHolderLocationAndLastHandover_inOneListCall() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        intake(resident, "CLOTHING", "\"quantity\": 2", location);
        long inUse = intake(resident, "CLOTHING", "\"quantity\": 1", location);
        long lost = intake(resident, "CLOTHING", "\"quantity\": 1", location);
        long damaged = intake(resident, "CLOTHING", "\"quantity\": 1", location);
        handover(tokenA, command("GIVE_FOR_USE", item(inUse, ""), "")).andExpect(status().isOk());
        handover(tokenA, command("REPORT_LOST", item(lost, ""), ", \"reason\": \"Không thấy trong tủ\""))
                .andExpect(status().isOk());
        handover(
                        tokenA,
                        command(
                                "RECORD_DAMAGE",
                                item(damaged, ", \"photoFileIds\": [" + photo() + "]"),
                                ", \"storageLocationId\": " + location + ", \"reason\": \"Rách\""))
                .andExpect(status().isOk());

        getJson(tokenA, "/api/v1/residents/" + resident + "/belongings")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].status", everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].lastHandoverAt", everyItem(notNullValue())))
                .andExpect(jsonPath("$[?(@.status != 'LOST')].holder", hasSize(3)))
                .andExpect(jsonPath("$[?(@.status != 'LOST')].holder", everyItem(notNullValue())))
                .andExpect(jsonPath("$[?(@.status == 'HELD' || @.status == 'DAMAGED')].storageLocationId", hasSize(2)))
                .andExpect(jsonPath(
                        "$[?(@.status == 'HELD' || @.status == 'DAMAGED')].storageLocationId",
                        everyItem(notNullValue())));
    }
}
