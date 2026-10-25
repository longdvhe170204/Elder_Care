package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 004 T022 (US2; FR-002, T-03): Từ chối và Hủy chờ đi qua lệnh Hủy tiếp nhận của 001 trong một lần; lệnh Hủy tiếp nhận
 * đến từ 001 thì hồ sơ chờ chuyển Hủy chờ mà không gọi lại 001.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_002_DeclineCancelAdmissionIT extends WaitingListTestBase {

    private static final String REASON = "{\"reason\":\"Gia đình không tiếp tục chờ\"}";

    private record Waiting(long residentId, long applicationId, long entryId) {}

    private Waiting waiting() throws Exception {
        long residentId = admissionResident(Gender.MALE);
        long applicationId = application(residentId, today(), "BASIC");
        long entryId = createEntry(applicationId).get("id").asLong();
        return new Waiting(residentId, applicationId, entryId);
    }

    private String applicationStatus(long applicationId) {
        return jdbc.queryForObject(
                "SELECT status FROM admission_application WHERE id = ?", String.class, applicationId);
    }

    private int audit(String action, long entryId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.WAITING_LIST_ENTRY' AND entity_id = ? AND action = ?",
                Integer.class,
                String.valueOf(entryId),
                action);
    }

    @Test
    void FR_002_decline_marksDeclined_cancelsApplication_andCancelsAdmissionOnce() throws Exception {
        Waiting w = waiting();

        mvc.perform(post("/api/v1/waiting-list-entries/" + w.entryId() + "/decline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REASON))
                .andExpect(status().isOk());

        assertThat(entry(w.entryId()).get("status").asString()).isEqualTo("DECLINED");
        assertThat(applicationStatus(w.applicationId())).isEqualTo("CANCELLED");
        assertThat(currentStatus(w.residentId())).isEqualTo(ResidentStatus.ADMISSION_CANCELLED);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_status_history WHERE resident_id = ? AND command ="
                                + " 'CANCEL_ADMISSION' AND reason = 'Gia đình không tiếp tục chờ'",
                        Integer.class,
                        w.residentId()))
                .as("đúng một lần Hủy tiếp nhận, cùng lý do")
                .isEqualTo(1);
        assertThat(audit("M02_WAITING_ENTRY_DECLINED", w.entryId())).isEqualTo(1);
    }

    @Test
    void FR_002_cancelWaiting_marksWaitingCancelled_andCancelsAdmission() throws Exception {
        Waiting w = waiting();

        mvc.perform(post("/api/v1/waiting-list-entries/" + w.entryId() + "/cancel-waiting")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REASON))
                .andExpect(status().isOk());

        assertThat(entry(w.entryId()).get("status").asString()).isEqualTo("WAITING_CANCELLED");
        assertThat(applicationStatus(w.applicationId())).isEqualTo("CANCELLED");
        assertThat(currentStatus(w.residentId())).isEqualTo(ResidentStatus.ADMISSION_CANCELLED);
        assertThat(audit("M02_WAITING_ENTRY_CANCELLED", w.entryId())).isEqualTo(1);
    }

    @Test
    void FR_002_cancelAdmissionFrom001_cancelsTheOpenEntryWithoutCallingBack() throws Exception {
        Waiting w = waiting();

        statusService.cancelAdmission(
                com.cambae.backend.m01.internal.status.service.ResidentStatusService.target(w.residentId()),
                new Reason("Gia đình rút hồ sơ"),
                JsonNodeFactory.instance.objectNode());

        assertThat(entry(w.entryId()).get("status").asString()).isEqualTo("WAITING_CANCELLED");
        assertThat(applicationStatus(w.applicationId())).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_status_history WHERE resident_id = ? AND command ="
                                + " 'CANCEL_ADMISSION'",
                        Integer.class,
                        w.residentId()))
                .isEqualTo(1);
    }

    @Test
    void FR_002_withoutReason_is422_andNothingChanges() throws Exception {
        Waiting w = waiting();

        mvc.perform(post("/api/v1/waiting-list-entries/" + w.entryId() + "/decline")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\" \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));

        assertThat(entry(w.entryId()).get("status").asString()).isEqualTo("WAITING");
        assertThat(currentStatus(w.residentId())).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
    }

    @Test
    void FR_002_aClosedEntry_cannotBeDeclinedOrCancelled() throws Exception {
        Waiting w = waiting();
        jdbc.update("UPDATE waiting_list_entry SET status = 'WAITING_CANCELLED' WHERE id = ?", w.entryId());

        for (String command : new String[] {"decline", "cancel-waiting"}) {
            mvc.perform(post("/api/v1/waiting-list-entries/" + w.entryId() + "/" + command)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REASON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("WAITING_ENTRY_CLOSED"));
        }
    }

    @Test
    void FR_002_onlyOfficeDeclines() throws Exception {
        Waiting w = waiting();

        for (var denied : new com.cambae.backend.support.TestUsers.TestUser[] {manager, doctor, nurse}) {
            user.set(denied.user());
            mvc.perform(post("/api/v1/waiting-list-entries/" + w.entryId() + "/decline")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REASON))
                    .andExpect(status().isForbidden());
        }
        assertThat(currentStatus(w.residentId())).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
    }
}
