package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.ContractStatusChanged;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T029 (US3; FR-019 → FR-023, FR-025, FR-027, BR-M02-09): vòng đời hợp đồng Nháp → Chờ ký → Hiệu lực, Trả về nháp,
 * Hủy; nội dung chỉ sửa được ở Nháp; chuyển không hợp lệ bị chặn; Hủy tiếp nhận (001) kéo theo hợp đồng.
 */
@AutoConfigureMockMvc
@WithMockUser
class ContractLifecycleIT extends ContractTestBase {

    @Test
    void FR_019_create_returnsDraftWithNumber_etag_andNoWarningsWhenConsistent() throws Exception {
        long residentId = residentWithRepresentative();

        MvcResult result = createRaw(body(residentId))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.contract.status").value("DRAFT"))
                .andExpect(jsonPath("$.contract.residentId").value(residentId))
                .andExpect(jsonPath("$.contract.awaitingTermsApproval").value(false))
                .andReturn();

        JsonNode created =
                JSON.readTree(result.getResponse().getContentAsString()).get("contract");
        assertThat(created.get("contractNumber").asString()).matches("HD-\\d{4}-\\d{6}");
        assertThat(created.get("allowedCommands").toString()).contains("send-for-signature", "cancel");
        assertThat(created.get("services")).hasSize(1);
        assertThat(JSON.readTree(result.getResponse().getContentAsString()).get("warnings"))
                .isEmpty();
        assertThat(contractStatus(created.get("id").asLong())).isEqualTo("DRAFT");
    }

    @Test
    void FR_019_create_withoutRepresentative_is409() throws Exception {
        long residentId = admissionResident();

        createRaw(body(residentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REPRESENTATIVE_REQUIRED"));
    }

    @Test
    void FR_019_create_forAFinalStatusResident_is409() throws Exception {
        long residentId = residentWithRepresentative();
        setStatus(residentId, "ADMISSION_CANCELLED");

        createRaw(body(residentId)).andExpect(status().isConflict());
    }

    @Test
    void FR_019_create_forAResidentInResidence_isAllowed_forTheNextContract() throws Exception {
        long residentId = residentWithRepresentative();
        setStatus(residentId, "IN_RESIDENCE");

        createRaw(body(residentId)).andExpect(status().isCreated());
    }

    @Test
    void FR_019_shortTermNeedsEndDate_dayCareNeedsValidSchedule_andInvalidInputsAre422() throws Exception {
        long residentId = residentWithRepresentative();

        ObjectNode shortTerm = body(residentId);
        shortTerm.put("residenceType", "SHORT_TERM_RESIDENTIAL");
        createRaw(shortTerm).andExpect(status().isUnprocessableContent());
        shortTerm.put("endDate", today().plusDays(14).toString());
        createRaw(shortTerm).andExpect(status().isCreated());

        ObjectNode endBeforeStart = body(residentId);
        endBeforeStart.put("endDate", today().minusDays(1).toString());
        createRaw(endBeforeStart).andExpect(status().isUnprocessableContent());

        ObjectNode backwards = body(residentId);
        backwards.put("residenceType", "DAY_CARE");
        backwards
                .putArray("arrivalSchedule")
                .addObject()
                .put("dayOfWeek", 1)
                .put("arrivalTime", "16:30")
                .put("departureTime", "07:30");
        createRaw(backwards).andExpect(status().isUnprocessableContent());

        ObjectNode scheduleOnInpatient = body(residentId);
        scheduleOnInpatient
                .putArray("arrivalSchedule")
                .addObject()
                .put("dayOfWeek", 1)
                .put("arrivalTime", "07:30")
                .put("departureTime", "16:30");
        createRaw(scheduleOnInpatient).andExpect(status().isUnprocessableContent());

        ObjectNode bothDeposit = body(residentId);
        bothDeposit.put("depositAmount", 10_000_000);
        createRaw(bothDeposit).andExpect(status().isUnprocessableContent());

        ObjectNode unknownService = body(residentId);
        unknownService.putArray("services").add(line(987_654_321L));
        createRaw(unknownService).andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_025_careLevelDifferentFromCurrent_warns_butDoesNotBlock() throws Exception {
        long residentId = residentWithRepresentative();
        confirmedAssessment(residentId, CareLevel.SPECIAL);

        MvcResult result =
                createRaw(body(residentId)).andExpect(status().isCreated()).andReturn();

        JsonNode warnings =
                JSON.readTree(result.getResponse().getContentAsString()).get("warnings");
        assertThat(warnings.toString()).contains("CONTRACT_CARE_LEVEL_MISMATCH");
    }

    @Test
    void FR_020_update_inDraft_changesContent_andNeedsMatchingEtag() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);
        String etag = etag(id);

        ObjectNode changed = body(residentId);
        changed.put("terminationCondition", "Báo trước 60 ngày");
        updateRaw(id, etag, changed)
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.contract.terminationCondition").value("Báo trước 60 ngày"));

        updateRaw(id, etag, changed).andExpect(status().isPreconditionFailed());
        assertThat(contract(id).get("terminationCondition").asString()).isEqualTo("Báo trước 60 ngày");
    }

    @Test
    void FR_020_update_cannotChangeTheResident() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);

        ObjectNode other = body(residentWithRepresentative());
        updateRaw(id, etag(id), other).andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_020_sendForSignature_locksContent_returnToDraftUnlocks() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);

        command(id, "send-for-signature")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contract.status").value("PENDING_SIGNATURE"));
        updateRaw(id, etag(id), body(residentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_LOCKED"));

        commandWithReason(id, "return-to-draft", "Gia đình xin sửa điều khoản").andExpect(status().isOk());
        assertThat(contractStatus(id)).isEqualTo("DRAFT");
        updateRaw(id, etag(id), body(residentId)).andExpect(status().isOk());
    }

    @Test
    void FR_020_returnToDraft_needsReason() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);
        send(id);

        command(id, "return-to-draft").andExpect(status().isBadRequest());
        commandWithReason(id, "return-to-draft", " ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        assertThat(contractStatus(id)).isEqualTo("PENDING_SIGNATURE");
    }

    @Test
    void FR_020_cancel_fromDraftAndPendingSignature_withReason() throws Exception {
        long residentId = residentWithRepresentative();
        long draft = createDraft(residentId);
        commandWithReason(draft, "cancel", "Gia đình đổi ý").andExpect(status().isOk());
        assertThat(contractStatus(draft)).isEqualTo("CANCELLED");

        long pending = createDraft(residentId);
        send(pending);
        commandWithReason(pending, "cancel", "Gia đình đổi ý").andExpect(status().isOk());
        assertThat(contractStatus(pending)).isEqualTo("CANCELLED");

        command(createDraft(residentId), "cancel").andExpect(status().isBadRequest());
    }

    @Test
    void FR_025_sendForSignature_needsExactlyOneCareServiceOfTheContractLevel() throws Exception {
        long residentId = residentWithRepresentative();

        ObjectNode noCare = body(residentId);
        noCare.putArray("services").add(line(meal));
        long none = create(noCare).get("contract").get("id").asLong();
        MvcResult missing = command(none, "send-for-signature")
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(unmetCodes(missing)).contains("CONTRACT_CARE_SERVICE_REQUIRED");

        ObjectNode wrongLevel = body(residentId);
        wrongLevel.putArray("services").add(line(regularCare));
        long wrong = create(wrongLevel).get("contract").get("id").asLong();
        assertThat(unmetCodes(command(wrong, "send-for-signature")
                        .andExpect(status().isConflict())
                        .andReturn()))
                .contains("CONTRACT_CARE_SERVICE_REQUIRED");

        ObjectNode twoCare = body(residentId);
        twoCare.putArray("services").add(line(basicCare)).add(line(regularCare));
        long two = create(twoCare).get("contract").get("id").asLong();
        assertThat(unmetCodes(command(two, "send-for-signature")
                        .andExpect(status().isConflict())
                        .andReturn()))
                .contains("CONTRACT_CARE_SERVICE_REQUIRED");

        assertThat(contractStatus(none)).isEqualTo("DRAFT");
    }

    @Test
    void FR_019_sendForSignature_blocksWhenMandatoryContentIsMissing() throws Exception {
        long residentId = residentWithRepresentative();

        ObjectNode incomplete = body(residentId);
        incomplete.remove("roomTypeId");
        incomplete.remove("noDepositRequired");
        long id = create(incomplete).get("contract").get("id").asLong();

        MvcResult result = command(id, "send-for-signature")
                .andExpect(status().isConflict())
                .andReturn();
        assertThat(unmetCodes(result)).contains("CONTRACT_ROOM_REQUIRED", "CONTRACT_DEPOSIT_REQUIRED");

        ObjectNode dayCare = body(residentId);
        dayCare.put("residenceType", "DAY_CARE");
        dayCare.remove("roomTypeId");
        long day = create(dayCare).get("contract").get("id").asLong();
        assertThat(unmetCodes(command(day, "send-for-signature")
                        .andExpect(status().isConflict())
                        .andReturn()))
                .contains("CONTRACT_ARRIVAL_SCHEDULE_REQUIRED");
    }

    @Test
    void FR_020_everyCommandOutsideItsStatus_isRefused() throws Exception {
        long residentId = admissionResident();
        for (ContractStatus status : List.of(
                ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED, ContractStatus.CANCELLED)) {
            long id = contracts.contract(
                    residentId, status, ResidenceType.LONG_TERM_RESIDENTIAL, CareLevel.BASIC, today());
            representatives.representative(residentId, representativeOf(residentId));

            updateRaw(id, "\"0\"", body(residentId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("CONTRACT_LOCKED"));
            for (String name : List.of("send-for-signature", "submit-terms-for-approval")) {
                commandWithReason(id, name, "thử")
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));
            }
            commandWithReason(id, "return-to-draft", "thử")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));
            commandWithReason(id, "cancel", "thử")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));
            recordSignedRaw(id, signedBody(residentId, today()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));
            assertThat(contractStatus(id)).isEqualTo(status.name());
        }
    }

    @Test
    void FR_020_draftCannotBeRecordedSigned_andPendingCannotBeSentAgain() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);

        recordSignedRaw(id, signedBody(residentId, today()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));
        send(id);
        command(id, "send-for-signature")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TRANSITION"));
    }

    @Test
    void FR_033a_statusChanges_publishContractStatusChanged() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);
        send(id);
        commandWithReason(id, "return-to-draft", "Sửa điều khoản").andExpect(status().isOk());
        send(id);
        recordSignedRaw(id, signedBody(residentId, today())).andExpect(status().isOk());

        List<ContractStatusChanged> changes = events.stream(ContractStatusChanged.class)
                .filter(event -> event.contractId() == id)
                .toList();
        assertThat(changes)
                .extracting(ContractStatusChanged::from, ContractStatusChanged::to)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE),
                        org.assertj.core.groups.Tuple.tuple(ContractStatus.PENDING_SIGNATURE, ContractStatus.DRAFT),
                        org.assertj.core.groups.Tuple.tuple(ContractStatus.DRAFT, ContractStatus.PENDING_SIGNATURE),
                        org.assertj.core.groups.Tuple.tuple(ContractStatus.PENDING_SIGNATURE, ContractStatus.ACTIVE));
        assertThat(changes.get(1).reason()).isEqualTo("Sửa điều khoản");
    }

    @Test
    void FR_002_cancelAdmission_cancelsDraftAndPendingContracts_andTerminatesTheActiveOne() throws Exception {
        long residentId = residentWithRepresentative();
        long active = activeContract(residentId, body(residentId));
        ObjectNode next = body(residentId);
        next.put("startDate", today().plusDays(400).toString());
        long draft = create(next).get("contract").get("id").asLong();
        long pending = create(next).get("contract").get("id").asLong();
        send(pending);

        statusService.cancelAdmission(
                ResidentStatusService.target(residentId),
                new Reason("Gia đình rút hồ sơ"),
                JsonNodeFactory.instance.objectNode());

        assertThat(contractStatus(draft)).isEqualTo("CANCELLED");
        assertThat(contractStatus(pending)).isEqualTo("CANCELLED");
        assertThat(contractStatus(active)).isEqualTo("TERMINATED");
        assertThat(jdbc.queryForObject("SELECT termination_reason FROM contract WHERE id = ?", String.class, active))
                .as("FR-027: lý do chấm dứt được lưu, lý do của lệnh nằm ở lịch sử trạng thái của 001")
                .startsWith("Hủy tiếp nhận");
    }

    @Test
    void FR_020_list_filtersByResidentAndStatus() throws Exception {
        long residentId = residentWithRepresentative();
        long draft = createDraft(residentId);
        long cancelled = createDraft(residentId);
        commandWithReason(cancelled, "cancel", "Trùng").andExpect(status().isOk());

        user.set(office.user());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/contracts")
                        .param("residentId", String.valueOf(residentId))
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(draft));
    }

    @Test
    void FR_028_onlyTheOfficeRunsContractCommands_everyoneElseIs403() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);
        var accountant = users.staff(com.cambae.backend.common.Role.ACCOUNTANT);

        for (var who : List.of(manager, accountant, nurse, doctor)) {
            user.set(who.user());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/contracts")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(body(residentId).toString()))
                    .andExpect(status().isForbidden());
            for (String name : List.of("send-for-signature", "cancel", "submit-terms-for-approval")) {
                mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/contracts/" + id + "/" + name)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"thử\"}"))
                        .andExpect(status().isForbidden());
            }
        }
        assertThat(contractStatus(id)).isEqualTo("DRAFT");
    }
}
