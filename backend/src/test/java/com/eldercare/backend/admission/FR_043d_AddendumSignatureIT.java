package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * FR-043d (Q-419, plan-bf08 Bước 3; R3 → R6, R17): duyệt yêu cầu không phải thay đổi nhỏ tạo phụ lục Chờ ký; chưa ký thì
 * không áp dụng, yêu cầu giữ Đã duyệt; ghi nhận bản đã ký (ngày, người đại diện, bản scan, người ký phía viện) chuyển
 * Chờ hiệu lực; ký muộn dời ngày hiệu lực; số phụ lục tăng dần theo hợp đồng, không tái sử dụng; trigger giữ nội dung.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_043d_AddendumSignatureIT extends ChangeTestBase {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.eldercare.backend.support.fake.administration.FakeStaffProfiles staffProfiles;

    /** Thêm dịch vụ chưa có trong hợp đồng: không phải thay đổi nhỏ (Q-338, R19). */
    private String addNew(long serviceId) {
        return "{\"services\": [{\"serviceId\": " + serviceId + ", \"action\": \"ADD\"}]}";
    }

    private long approvedNonMinor(Setup s, LocalDate effective) throws Exception {
        long extra = service("OTHER", "Giặt là riêng " + System.nanoTime(), null, 300_000);
        return createApproveUnsigned(request(s.contractId(), "[\"SERVICE\"]", addNew(extra), effective));
    }

    @Test
    void R3_R4_notMinor_pendingSignature_notAppliedOnEffectiveDate() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(2);
        long id = approvedNonMinor(s, effective);

        assertThat(addendumStatus(id)).isEqualTo("PENDING_SIGNATURE");
        JsonNode addendum = addenda(s.contractId()).get(0);
        assertThat(addendum.get("status").asString()).isEqualTo("PENDING_SIGNATURE");
        assertThat(addendum.get("signatureRequired").asBoolean()).isTrue();

        applyDueOn(effective);

        assertThat(requestStatus(id)).isEqualTo("APPROVED");
        assertThat(addendumStatus(id)).isEqualTo("PENDING_SIGNATURE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M15.APPROVAL_REQUEST' AND entity_id = ?"
                                + " AND action = 'REQUEST_APPLY_FAILED'",
                        Integer.class,
                        String.valueOf(id)))
                .isZero();
        assertThat(versionCount(s.contractId())).isEqualTo(1);
    }

    @Test
    void R5_signBeforeEffectiveDate_appliedOnThatDate() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(3);
        long id = approvedNonMinor(s, effective);

        sign(id, today())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addendum.status").value("PENDING_EFFECTIVE"))
                .andExpect(jsonPath("$.addendum.signedDate").value(today().toString()));

        applyDueOn(effective.minusDays(1));
        assertThat(requestStatus(id)).isEqualTo("APPROVED");
        applyDueOn(effective);
        assertThat(requestStatus(id)).isEqualTo("APPLIED");
        assertThat(addendumStatus(id)).isEqualTo("APPLIED");
    }

    @Test
    void R6_signedAfterEffectiveDate_effectiveDateMovesToSigningDay() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(1);
        long id = approvedNonMinor(s, effective);

        applyDueOn(effective.plusDays(2));
        assertThat(requestStatus(id)).isEqualTo("APPROVED");

        LocalDate signed = effective.plusDays(2);
        sign(id, signed)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addendum.effectiveDate").value(signed.toString()));
        assertThat(jdbc.queryForObject(
                        "SELECT actual_effective_date FROM approval_request WHERE id = ?", LocalDate.class, id))
                .isEqualTo(signed);

        applyDueOn(signed);
        assertThat(requestStatus(id)).isEqualTo("APPLIED");
        assertThat(jdbc.queryForObject(
                        "SELECT effective_from FROM contract_version WHERE kind = 'CONTENT' AND contract_id = ?"
                                + " ORDER BY sequence_no DESC LIMIT 1",
                        LocalDate.class,
                        s.contractId()))
                .as("không tính lùi (FR-044)")
                .isEqualTo(signed);
    }

    @Test
    void R3_minorChange_pendingEffectiveWithoutSignature() throws Exception {
        Setup s = activeSetup();
        String removeMeal = "{\"services\": [{\"serviceId\": " + meal + ", \"action\": \"REMOVE\"}]}";
        long id = createApproveUnsigned(request(s.contractId(), "[\"SERVICE\"]", removeMeal, today().plusDays(2)));

        assertThat(addendumStatus(id)).isEqualTo("PENDING_EFFECTIVE");
        sign(id, today())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ADDENDUM_NOT_PENDING_SIGNATURE"));
    }

    @Test
    void R5_invalidSigner_futureDate_secondSignature_rejected() throws Exception {
        Setup s = activeSetup();
        long id = approvedNonMinor(s, today().plusDays(2));
        Setup other = activeSetup();

        signRaw(id, today(), representativeOf(other.residentId()), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SIGNER_NOT_REPRESENTATIVE"));
        sign(id, today().plusDays(1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("SIGNED_DATE_INVALID"));
        sign(id, today().minusDays(1))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("SIGNED_DATE_INVALID"));

        sign(id, today()).andExpect(status().isOk());
        sign(id, today())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ADDENDUM_NOT_PENDING_SIGNATURE"));
    }

    @Test
    void R17_facilitySignatory_recordedAndValidated() throws Exception {
        Setup s = activeSetup();
        long id = approvedNonMinor(s, today().plusDays(2));

        signRaw(id, today(), representativeOf(s.residentId()), 987_654_321L)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("FACILITY_SIGNATORY_INVALID"));
        long staffId = com.eldercare.backend.support.TestUsers.nextId();
        jdbc.update(
                "INSERT INTO staff_profile (id, full_name, position) VALUES (?, 'Lê Ký Viện', 'Hành chính')", staffId);
        if (staffProfiles != null) {
            // Context test dùng bản giả @Primary của 002 (FakeM15Config): hồ sơ có họ tên mới là nhân viên có thật
            staffProfiles.name(staffId, "Lê Ký Viện");
        }
        signRaw(id, today(), representativeOf(s.residentId()), staffId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addendum.facilitySignatoryStaffId").value(staffId));
    }

    @Test
    void R17_addendumNumbers_increasePerContract_neverReused() throws Exception {
        Setup s = activeSetup();
        long first = approvedNonMinor(s, today().plusDays(2));
        user.set(manager.user());
        generic(first, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());
        assertThat(addendumStatus(first)).isEqualTo("CANCELLED");

        long second = approvedNonMinor(s, today().plusDays(3));
        // Yêu cầu Đã duyệt đang giữ thuộc tính Dịch vụ (FR-046): hủy trước khi duyệt yêu cầu sau
        user.set(manager.user());
        generic(second, "cancel", "{\"reason\":\"Gia đình đổi ý\"}").andExpect(status().isOk());
        long third = approvedNonMinor(s, today().plusDays(4));

        assertThat(getChange(first).get("addendum").get("addendumNo").asInt()).isEqualTo(1);
        assertThat(getChange(second).get("addendum").get("addendumNo").asInt()).isEqualTo(2);
        assertThat(getChange(third).get("addendum").get("addendumNo").asInt()).isEqualTo(3);
    }

    @Test
    void R17_trigger_keepsContentAndNumberImmutable() throws Exception {
        Setup s = activeSetup();
        long id = approvedNonMinor(s, today().plusDays(2));

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract_version SET content_after = JSON_OBJECT('x', 1) WHERE kind = 'ADDENDUM'"
                                + " AND approval_request_id = ?",
                        id))
                .hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract_version SET addendum_no = 99 WHERE kind = 'ADDENDUM' AND approval_request_id = ?",
                        id))
                .hasMessageContaining("immutable");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract_version SET effective_date = DATE_SUB(effective_date, INTERVAL 1 DAY)"
                                + " WHERE kind = 'ADDENDUM' AND approval_request_id = ?",
                        id))
                .hasMessageContaining("later");
    }
}
