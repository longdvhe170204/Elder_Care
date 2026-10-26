package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.events.ResidencyEndingCancelled;
import com.cambae.backend.m02.api.events.ResidencyEndingCreated;
import com.cambae.backend.m02.api.events.ResidencyEndingDateChanged;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T057 (US7; FR-062, FR-066, FR-067; contracts/api-004.yaml, tag {@code residency-ending}): lập, xem, đổi ngày, hủy và
 * ghi nhận bàn giao của hồ sơ kết thúc lưu trú. Mỗi người tối đa một hồ sơ Đang chuẩn bị; Chuyển cơ sở bắt buộc nơi chuyển
 * đến; Bác sĩ chỉ lập Xuất viện, Chuyển cơ sở vì lý do y tế.
 */
@AutoConfigureMockMvc
@WithMockUser
class ResidencyEndingIT extends EndingTestBase {

    @Test
    void FR_062_createEnding_storesThePreparingRecord_callsFinalPeriod_andPublishesTheEvent() throws Exception {
        long residentId = inResidence();
        LocalDate expected = d0.plusDays(10);

        JsonNode created = JSON.readTree(createEndingRaw(office, endingBody(residentId, "DISCHARGE", expected))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PREPARING"))
                .andExpect(jsonPath("$.endCase").value("DISCHARGE"))
                .andExpect(jsonPath("$.expectedEndDate").value(expected.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString());

        long id = created.get("id").asLong();
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM residency_ending WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("PREPARING");
        assertThat(row.get("requested_by")).isEqualTo("Con gái ông A");
        assertThat(row.get("created_by")).isEqualTo(office.accountId());
        assertThat(finalPeriods.calls()).containsExactly("createDraft:" + residentId + ":" + expected);
        assertThat(row.get("final_period_ref"))
                .as("mã bảng kỳ cuối nháp do 010 trả")
                .isNotNull();
        assertThat(events.stream(ResidencyEndingCreated.class).filter(e -> e.residencyEndingId() == id))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ENDING_CREATED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(id)))
                .isEqualTo(1);
        assertThat(residentStatus(residentId))
                .as("lập hồ sơ chưa đổi trạng thái")
                .isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_062_onlyOnePreparingEndingPerResident_butANewOneAfterCancel() throws Exception {
        long residentId = inResidence();
        long first = createEnding(residentId, d0.plusDays(5));

        createEndingRaw(office, endingBody(residentId, "DISCHARGE", d0.plusDays(6)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENCY_ENDING_ALREADY_OPEN"));
        cancelRaw(office, first, "Gia đình đổi ý").andExpect(status().isOk());

        createEnding(residentId, d0.plusDays(8));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM residency_ending WHERE resident_id = ?", Integer.class, residentId))
                .isEqualTo(2);
    }

    @Test
    void FR_062_transferToAnotherFacility_requiresTheDestination() throws Exception {
        long residentId = inResidence();
        ObjectNode body = endingBody(residentId, "TRANSFER_TO_ANOTHER_FACILITY", d0.plusDays(5));

        createEndingRaw(office, body).andExpect(status().isUnprocessableEntity());
        body.put("destination", "Viện dưỡng lão Hoa Sen");

        createEndingRaw(office, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination").value("Viện dưỡng lão Hoa Sen"));
    }

    @Test
    void FR_062_aDoctorCreatesOnlyDischargeOrTransferForMedicalReasons() throws Exception {
        long residentId = inResidence();
        ObjectNode medical = endingBody(residentId, "DISCHARGE", d0.plusDays(5));

        createEndingRaw(doctor, medical).andExpect(status().isForbidden());
        medical.put("medicalReason", true);
        ObjectNode expired = endingBody(residentId, "CONTRACT_EXPIRED", d0.plusDays(5));
        expired.put("medicalReason", true);
        createEndingRaw(doctor, expired).andExpect(status().isForbidden());

        createEndingRaw(doctor, medical).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject(
                        "SELECT medical_reason FROM residency_ending WHERE resident_id = ?", Boolean.class, residentId))
                .isTrue();
    }

    @Test
    void FR_062_theResidentMustBeInResidence_temporarilyAbsentOrHospitalized() throws Exception {
        long admitting = admissionResident();
        createEndingRaw(office, endingBody(admitting, "DISCHARGE", d0.plusDays(5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESIDENT_NOT_IN_RESIDENCE"));

        long absent = inResidence();
        clockAt(d0, 10, 0);
        grant(absent, "HOME_VISIT", at(d0.plusDays(3), 10, 0));
        assertThat(residentStatus(absent)).isEqualTo(ResidentStatus.TEMPORARILY_ABSENT);
        createEndingRaw(office, endingBody(absent, "CONTRACT_EXPIRED", d0.plusDays(5)))
                .andExpect(status().isCreated());

        long hospital = inResidence();
        transferOut(hospital);
        assertThat(residentStatus(hospital)).isEqualTo(ResidentStatus.HOSPITALIZED);
        createEndingRaw(office, endingBody(hospital, "DISCHARGE", d0.plusDays(5)))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_067_cancelNeedsAReason_movesToCancelled_andPublishesTheEvent() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(5));

        postAs(office, "/api/v1/residency-endings/" + id + "/cancel", "{}").andExpect(status().isBadRequest());
        cancelRaw(office, id, "Gia đình đổi ý").andExpect(status().isOk());

        assertThat(endingStatus(id)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM residency_ending WHERE id = ?", String.class, id))
                .isEqualTo("Gia đình đổi ý");
        assertThat(finalPeriods.calls()).last().isEqualTo("cancelStop:" + residentId);
        assertThat(events.stream(ResidencyEndingCancelled.class).filter(e -> e.residencyEndingId() == id))
                .hasSize(1);
        cancelRaw(office, id, "Lần hai").andExpect(status().isConflict());
        changeDateRaw(id, d0.plusDays(9), "Đổi ngày").andExpect(status().isConflict());
        handoverRaw(office, id, handoverBody()).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM resident_handover WHERE ending_id = ?", Integer.class, id))
                .isZero();
    }

    @Test
    void FR_067_changeDate_needsAReason_recalculatesTheFinalPeriod_andPublishesTheEvent() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(5));

        postAs(
                        office,
                        "/api/v1/residency-endings/" + id + "/change-date",
                        "{\"expectedEndDate\":\"" + d0.plusDays(8) + "\"}")
                .andExpect(status().isBadRequest());
        changeDateRaw(id, d0.plusDays(8), "Gia đình xin dời ngày").andExpect(status().isOk());

        assertThat(ending(id).get("expectedEndDate").asString())
                .isEqualTo(d0.plusDays(8).toString());
        assertThat(finalPeriods.calls()).last().isEqualTo("recalculate:" + residentId + ":" + d0.plusDays(8));
        assertThat(events.stream(ResidencyEndingDateChanged.class).filter(e -> e.residencyEndingId() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.expectedEndDate()).isEqualTo(d0.plusDays(8)));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ENDING_DATE_CHANGED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(id)))
                .isEqualTo(1);
    }

    @Test
    void FR_063_handover_isRecordedAppendOnly_andShownOnTheEnding() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(5));

        ObjectNode noFamily = handoverBody();
        noFamily.remove("familyMemberId");
        handoverRaw(office, id, noFamily).andExpect(status().isUnprocessableEntity());
        ObjectNode noFacility = handoverBody();
        noFacility.put("recipientType", "RECEIVING_FACILITY");
        handoverRaw(office, id, noFacility).andExpect(status().isUnprocessableEntity());

        ObjectNode facility = handoverBody();
        facility.put("recipientType", "RECEIVING_FACILITY");
        facility.remove("familyMemberId");
        facility.put("facilityName", "Bệnh viện Lão khoa");
        handoverRaw(office, id, facility).andExpect(status().isCreated());

        JsonNode handover = ending(id).get("handover");
        assertThat(handover.get("recipientType").asString()).isEqualTo("RECEIVING_FACILITY");
        assertThat(handover.get("recipient").asString()).isEqualTo("Bệnh viện Lão khoa");
        assertThat(handover.get("staff").asString()).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM resident_handover WHERE ending_id = ?", Integer.class, id))
                .isEqualTo(1);
        org.junit.jupiter.api.Assertions.assertThrows(
                java.sql.SQLException.class,
                () -> WaitingListTestBase.admin(
                        "UPDATE resident_handover SET facility_name = 'x' WHERE ending_id = " + id));
    }

    @Test
    void FR_062_theDatabase_guaranteesOnePreparingEnding_theDestination_andTheClosedRecord() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(5));
        String insert = "INSERT INTO residency_ending (resident_id, end_case, expected_end_date, requested_by, reason,"
                + " created_by, created_at) VALUES (%d, '%s', '%s', 'x', 'x', 1, NOW(6))";

        assertThrows(
                SQLException.class,
                () -> WaitingListTestBase.admin(insert.formatted(residentId, "DISCHARGE", d0.plusDays(6))));
        long other = inResidence();
        assertThrows(
                SQLException.class,
                () -> WaitingListTestBase.admin(
                        insert.formatted(other, "TRANSFER_TO_ANOTHER_FACILITY", d0.plusDays(6))));

        cancelRaw(office, id, "Đổi ý").andExpect(status().isOk());
        assertThrows(
                SQLException.class,
                () -> WaitingListTestBase.admin("UPDATE residency_ending SET reason = 'sửa' WHERE id = " + id));
        assertThrows(
                SQLException.class, () -> WaitingListTestBase.admin("DELETE FROM residency_ending WHERE id = " + id));
        assertThrows(
                SQLException.class,
                () -> WaitingListTestBase.admin(
                        "INSERT INTO ending_condition_exception (ending_id, resident_id, condition_code,"
                                + " approval_request_id, created_at) VALUES (NULL, " + residentId
                                + ", 'END_A_COSTS', 7, NOW(6))"));
    }

    @Test
    void FR_062_unknownEnding_is404_andAFamilyMemberCannotCreate() throws Exception {
        user.set(office.user());
        postAs(office, "/api/v1/residency-endings/999999999/cancel", "{\"reason\":\"x\"}")
                .andExpect(status().isNotFound());
        long residentId = inResidence();
        createEndingRaw(users.staff(com.cambae.backend.common.Role.CAREGIVER), endingBody(residentId, "DISCHARGE", d0))
                .andExpect(status().isForbidden());
    }
}
