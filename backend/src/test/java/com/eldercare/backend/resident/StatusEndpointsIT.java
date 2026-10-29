package com.eldercare.backend.resident;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.Condition;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T050 (US4; contracts/api-001.yaml, tag {@code status}): đường HTTP của lệnh trạng thái. Thân thiếu lý do 422,
 * điều kiện chưa thỏa 409 {@code UNMET_CONDITIONS} liệt kê đủ, cặp ngoài bảng 409 {@code INVALID_TRANSITION}, lệnh
 * thành công trả trạng thái mới kèm dòng lịch sử; {@code allowedCommands} chỉ gồm lệnh hợp trạng thái và vai trò.
 */
@AutoConfigureMockMvc
@WithMockUser
class StatusEndpointsIT extends StatusTestBase {

    @Autowired
    MockMvc mvc;

    private static final String BODY = "{\"reason\":\"Gia đình đón về cuối tuần\",\"details\":{}}";

    @Test
    void US4_grantTemporaryAbsence_returnsNewStatusAndHistory() throws Exception {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        mvc.perform(post("/api/v1/residents/" + id + "/grant-temporary-absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TEMPORARILY_ABSENT"))
                .andExpect(jsonPath("$.history.command").value("GRANT_TEMPORARY_ABSENCE"))
                .andExpect(jsonPath("$.history.from").value("IN_RESIDENCE"))
                .andExpect(jsonPath("$.history.reason").value("Gia đình đón về cuối tuần"))
                .andExpect(jsonPath("$.history.bySystem").value(false));

        mvc.perform(get("/api/v1/residents/" + id + "/status-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[-1:].to").value("TEMPORARILY_ABSENT"));
    }

    @Test
    void US4_missingReason_is422() throws Exception {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        mvc.perform(post("/api/v1/residents/" + id + "/grant-temporary-absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"details\":{}}"))
                .andExpect(status().is(400));
        mvc.perform(post("/api/v1/residents/" + id + "/grant-temporary-absence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
    }

    @Test
    void FR_045_unmetConditions_listedInTheProblemBody_andPreviewMatches() throws Exception {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        one.unmet(new Condition("BELONGINGS_NOT_RETURNED", "Còn đồ gửi", "BR-M12-03", false));
        two.unmet(new Condition("MEDICINE_NOT_RETURNED", "Còn thuốc gửi", "BR-M07-01", true));
        user.set(office.user());

        mvc.perform(post("/api/v1/residents/" + id + "/end-residency")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("UNMET_CONDITIONS"))
                .andExpect(jsonPath("$.unmetConditions", hasSize(2)));

        mvc.perform(get("/api/v1/residents/" + id + "/conditions/END_RESIDENCY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].code", hasItems("BELONGINGS_NOT_RETURNED", "MEDICINE_NOT_RETURNED")));
        mvc.perform(get("/api/v1/residents/" + id + "/conditions/ROLL_CALL_DEPARTURE"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_044_pairOutsideTheTable_is409InvalidTransition() throws Exception {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        mvc.perform(post("/api/v1/residents/" + id + "/record-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));
    }

    @Test
    void FR_047c_attachmentsAreRefusedUntilFileStorageExists() throws Exception {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        user.set(office.user());

        mvc.perform(post("/api/v1/residents/" + id + "/record-return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Về viện\",\"attachmentFileIds\":[1]}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("ATTACHMENTS_NOT_ACCEPTED"));
    }

    @Test
    void US4_allowedCommands_followStatusAndRole() throws Exception {
        long living = residentIn(ResidentStatus.IN_RESIDENCE);
        long absent = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        long closed = residentIn(ResidentStatus.DECEASED);
        assignNurse(nurse, living);

        user.set(office.user());
        mvc.perform(get("/api/v1/residents/" + living))
                .andExpect(jsonPath("$.allowedCommands", hasItems("GRANT_TEMPORARY_ABSENCE", "END_RESIDENCY")))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("RECORD_RETURN"))))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("COMPLETE_ADMISSION"))))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("TRANSFER_OUT"))))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("RECORD_DEATH"))));
        mvc.perform(get("/api/v1/residents/" + absent))
                .andExpect(jsonPath("$.allowedCommands", hasItems("RECORD_RETURN", "RECORD_DEATH")));
        mvc.perform(get("/api/v1/residents/" + closed))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("END_RESIDENCY"))));

        user.set(doctor.user());
        mvc.perform(get("/api/v1/residents/" + living))
                .andExpect(jsonPath("$.allowedCommands", hasItems("TRANSFER_OUT", "RECORD_DEATH", "END_RESIDENCY")))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("GRANT_TEMPORARY_ABSENCE"))));

        user.set(nurse.user());
        mvc.perform(get("/api/v1/residents/" + living))
                .andExpect(jsonPath("$.allowedCommands", hasItem("TRANSFER_OUT")))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("END_RESIDENCY"))));
    }
}
