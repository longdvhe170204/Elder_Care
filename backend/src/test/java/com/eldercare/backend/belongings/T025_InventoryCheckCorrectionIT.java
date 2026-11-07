package com.eldercare.backend.belongings;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 013 T025 (FR-031a, research K-05): phiếu kiểm kê đã Hoàn thành là nhóm 3, chỉ đính chính theo FR-011a, nên chỉ sửa
 * ghi chú từng dòng ({@code lineNotes}); Hủy ghi nhận không hỗ trợ vì bản ghi bàn giao phiếu sinh ra có đính chính riêng.
 */
class T025_InventoryCheckCorrectionIT extends HandoverTestBase {

    private static final String URL = "/api/v1/belonging-inventory-checks";
    private static final String CORRECTIONS = "/api/v1/corrections/M12.INVENTORY_CHECK/";

    private long create(long location) throws Exception {
        String body = postJson(tokenA, URL, "{\"storageLocationId\": " + location + "}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long completedCheck(long belongingId, long location) throws Exception {
        long id = create(location);
        postJson(
                        tokenA,
                        URL + "/" + id + "/complete",
                        "{\"lines\": [{\"belongingId\": " + belongingId + ", \"result\": \"MATCHED\"}]}")
                .andExpect(status().isOk());
        return id;
    }

    private ResultActions amend(String token, long id, String content) throws Exception {
        return postJson(
                token,
                CORRECTIONS + id,
                "{\"type\": \"AMEND_CONTENT\", \"correctedContent\": " + content + ", \"reason\": \"Ghi sai\"}");
    }

    @Test
    void completedCheck_notesCanBeAmended_andChainShowsOriginalContent() throws Exception {
        long location = location();
        long phone = intake(resident("IN_RESIDENCE"), "PHONE", "\"quantity\": 1", location);
        long id = completedCheck(phone, location);

        amend(tokenA, id, "{\"lineNotes\": {\"" + phone + "\": \"Đã đối chiếu lại\"}}")
                .andExpect(status().is2xxSuccessful());

        getJson(tokenA, CORRECTIONS + id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalContent.lineNotes").exists());
    }

    @Test
    void waitingCheck_isNotConfirmed_soCannotBeCorrected() throws Exception {
        long location = location();
        long phone = intake(resident("IN_RESIDENCE"), "PHONE", "\"quantity\": 1", location);
        long id = create(location);

        amend(tokenA, id, "{\"lineNotes\": {\"" + phone + "\": \"x\"}}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECORD_NOT_CONFIRMED"));
    }

    @Test
    void voidingIsNotSupported() throws Exception {
        long location = location();
        long phone = intake(resident("IN_RESIDENCE"), "PHONE", "\"quantity\": 1", location);
        long id = completedCheck(phone, location);

        postJson(tokenA, CORRECTIONS + id, "{\"type\": \"VOID\", \"reason\": \"Ghi nhầm\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CORRECTION_NOT_SUPPORTED"));
    }

    @Test
    void onlyOfficeStaffMayCorrect() throws Exception {
        long location = location();
        long phone = intake(resident("IN_RESIDENCE"), "PHONE", "\"quantity\": 1", location);
        long id = completedCheck(phone, location);

        amend(nurse, id, "{\"lineNotes\": {\"" + phone + "\": \"x\"}}").andExpect(status().isForbidden());
    }

    @Test
    void invalidContent_isRejected() throws Exception {
        long location = location();
        long phone = intake(resident("IN_RESIDENCE"), "PHONE", "\"quantity\": 1", location);
        long id = completedCheck(phone, location);

        amend(tokenA, id, "{\"status\": \"WAITING_CHECK\"}").andExpect(status().isUnprocessableContent());
        amend(tokenA, id, "{\"lineNotes\": {}}").andExpect(status().isUnprocessableContent());
        amend(tokenA, id, "{\"lineNotes\": {\"" + phone + "\": 5}}").andExpect(status().isUnprocessableContent());
    }

    @Test
    void noteForABelongingOutsideTheCheck_isRejected() throws Exception {
        long location = location();
        long phone = intake(resident("IN_RESIDENCE"), "PHONE", "\"quantity\": 1", location);
        long id = completedCheck(phone, location);

        amend(tokenA, id, "{\"lineNotes\": {\"" + (phone + 99999) + "\": \"x\"}}")
                .andExpect(status().isUnprocessableContent());
    }
}
