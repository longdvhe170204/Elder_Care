package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/**
 * 004 T020 (US2; FR-011, SC-004, US2 #3, #11): Hành chính lập đề nghị điều chỉnh điểm (yêu cầu {@code
 * M02.SCORE_ADJUSTMENT}), Quản lý viện duyệt; điểm chỉ đổi khi yêu cầu được áp dụng; lịch sử lưu người đề nghị, người
 * duyệt, lý do.
 */
@AutoConfigureMockMvc
@WithMockUser
class ScoreAdjustmentIT extends WaitingListTestBase {

    private static final String REASON = "Vừa ra viện, không người chăm";

    private JsonNode propose(long entryId, String body) throws Exception {
        user.set(office.user());
        MvcResult result = mvc.perform(post("/api/v1/waiting-list-entries/" + entryId + "/propose-score-adjustment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private String requestEtag(long requestId) throws Exception {
        return mvc.perform(get("/api/v1/approval-requests/" + requestId))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
    }

    private void approve(long requestId) throws Exception {
        user.set(manager.user());
        mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                        .with(csrf())
                        .header("If-Match", requestEtag(requestId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Đồng ý\"}"))
                .andExpect(status().isOk());
        user.set(office.user());
    }

    @Test
    void US2_3_proposal_leavesScoreUnchangedUntilManagerApproves() throws Exception {
        setScoreTable(SCORE_TABLE);
        long id = newEntry(Gender.FEMALE, today(), "SPECIAL", "LIVING_ALONE")
                .get("id")
                .asLong();
        assertThat(entry(id).get("score").asInt()).isEqualTo(7);

        JsonNode ref = propose(id, "{\"points\":3,\"reason\":\"" + REASON + "\"}");

        assertThat(ref.get("type").asString()).isEqualTo("M02.SCORE_ADJUSTMENT");
        assertThat(ref.get("status").asString()).isEqualTo("PENDING_APPROVAL");
        assertThat(entry(id).get("score").asInt()).as("chưa được duyệt").isEqualTo(7);

        approve(ref.get("approvalRequestId").asLong());

        JsonNode after = entry(id);
        assertThat(after.get("score").asInt()).isEqualTo(10);
        assertThat(after.get("scoreBreakdown").get("adjustment").asInt()).isEqualTo(3);
        JsonNode history = scoreHistory(id);
        JsonNode last = history.get(history.size() - 1);
        assertThat(last.get("cause").asString()).isEqualTo("ADJUSTMENT");
        assertThat(last.get("oldScore").asInt()).isEqualTo(7);
        assertThat(last.get("newScore").asInt()).isEqualTo(10);
        assertThat(last.get("reason").asString()).isEqualTo(REASON);
        assertThat(last.get("proposedBy").asString()).isNotBlank();
        assertThat(last.get("approver").asString()).isNotBlank();
        assertThat(last.get("at").asString()).isNotBlank();
    }

    @Test
    void FR_011_adjustmentSurvivesDailyRecalculation() throws Exception {
        setScoreTable(SCORE_TABLE);
        noonOf(today());
        long id = newEntry(Gender.FEMALE, today(), "BASIC").get("id").asLong();
        approve(propose(id, "{\"points\":3,\"reason\":\"" + REASON + "\"}")
                .get("approvalRequestId")
                .asLong());

        noonOf(today().plusDays(7));
        dailyRunner.catchUp();

        assertThat(entry(id).get("score").asInt())
                .as("0 + 1 chu kỳ chờ + 3 điều chỉnh")
                .isEqualTo(4);
    }

    @Test
    void US2_11_rejectedProposal_changesNothing() throws Exception {
        long id = newEntry(Gender.FEMALE, today(), "REGULAR").get("id").asLong();
        int before = entry(id).get("score").asInt();
        long requestId = propose(id, "{\"points\":3,\"reason\":\"" + REASON + "\"}")
                .get("approvalRequestId")
                .asLong();

        user.set(manager.user());
        mvc.perform(post("/api/v1/approval-requests/" + requestId + "/reject")
                        .with(csrf())
                        .header("If-Match", requestEtag(requestId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Chưa đủ căn cứ\"}"))
                .andExpect(status().isOk());

        user.set(office.user());
        assertThat(entry(id).get("score").asInt()).isEqualTo(before);
        assertThat(scoreHistory(id)).hasSize(1);
    }

    @Test
    void US2_11_negativeAdjustment_onAContactedEntry_keepsItContacted() throws Exception {
        long id = newEntry(Gender.MALE, today(), "SPECIAL").get("id").asLong();
        jdbc.update("UPDATE waiting_list_entry SET status = 'CONTACTED', held_bed_id = ? WHERE id = ?", 777_002L, id);
        int before = entry(id).get("score").asInt();

        approve(propose(id, "{\"points\":-2,\"reason\":\"Gia đình đã có người chăm\"}")
                .get("approvalRequestId")
                .asLong());

        JsonNode after = entry(id);
        assertThat(after.get("score").asInt()).isEqualTo(before - 2);
        assertThat(after.get("status").asString()).isEqualTo("CONTACTED");
        assertThat(after.get("heldBedId").asLong()).isEqualTo(777_002L);
    }

    @Test
    void FR_011_invalidProposals_are422() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        user.set(office.user());

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/propose-score-adjustment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":3,\"reason\":\"  \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/propose-score-adjustment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":0,\"reason\":\"" + REASON + "\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_011_proposalOnAClosedEntry_is409() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        jdbc.update("UPDATE waiting_list_entry SET status = 'DECLINED' WHERE id = ?", id);

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/propose-score-adjustment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"points\":3,\"reason\":\"" + REASON + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WAITING_ENTRY_CLOSED"));
    }

    @Test
    void FR_011_entryClosedBeforeApproval_failsToApply_andScoreStays() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        long requestId = propose(id, "{\"points\":3,\"reason\":\"" + REASON + "\"}")
                .get("approvalRequestId")
                .asLong();
        jdbc.update("UPDATE waiting_list_entry SET status = 'WAITING_CANCELLED' WHERE id = ?", id);

        user.set(manager.user());
        mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                        .with(csrf())
                        .header("If-Match", requestEtag(requestId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLY_FAILED"))
                .andExpect(jsonPath("$.closingReason").value(org.hamcrest.Matchers.containsString("FR-011")));

        user.set(office.user());
        assertThat(entry(id).get("score").asInt()).isZero();
    }

    @Test
    void FR_011_onlyOfficeProposes_managerAndOthersAreDenied() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        String body = "{\"points\":3,\"reason\":\"" + REASON + "\"}";

        for (var denied : new com.cambae.backend.support.TestUsers.TestUser[] {manager, doctor, nurse}) {
            user.set(denied.user());
            mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/propose-score-adjustment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void FR_011_historyIsVisibleToOfficeAndManager() throws Exception {
        long id = newEntry(Gender.MALE, today(), "BASIC").get("id").asLong();
        user.set(manager.user());

        mvc.perform(get("/api/v1/waiting-list-entries/" + id + "/score-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cause").value("CREATED"));
    }
}
