package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T042 (DBR-07, FR-040, FR-047): phụ lục chỉ do yêu cầu đã duyệt tạo, ngày hiệu lực không sớm hơn ngày bắt đầu hợp đồng
 * gốc, không có thao tác sửa hay xóa phụ lục; cơ sở dữ liệu giữ các quy tắc này khi mã bị bỏ qua.
 */
@AutoConfigureMockMvc
@WithMockUser
class DBR_07_AddendumIT extends ChangeTestBase {

    private static final String INSERT_ADDENDUM = "INSERT INTO contract_addendum (contract_id, approval_request_id,"
            + " change_types, content_before, content_after, effective_date, created_at)"
            + " VALUES (?, ?, 'CARE_LEVEL', '{}', '{}', ?, NOW(6))";

    @Test
    void FR_040_effectiveDateBeforeTheContractStart_is422() throws Exception {
        Setup s = activeSetup();
        user.set(office.user());

        createRaw(
                        "/api/v1/residency-change-requests",
                        request(
                                s.contractId(),
                                "[\"CARE_LEVEL\"]",
                                "{\"careLevel\": \"REGULAR\"}",
                                today().minusDays(1)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("EFFECTIVE_DATE_BEFORE_CONTRACT_START"));
    }

    @Test
    void DBR_07_changeRequestOnAContractThatIsNotActive_is409() throws Exception {
        long residentId = residentWithRepresentative();
        long draft = createDraft(residentId);
        user.set(office.user());

        createRaw(
                        "/api/v1/residency-change-requests",
                        request(draft, "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_ACTIVE"));
    }

    @Test
    void DBR_07_theDatabaseRejectsAnAddendumDatedBeforeTheContractStart() throws Exception {
        Setup s = activeSetup();

        assertThatThrownBy(() -> jdbc.update(INSERT_ADDENDUM, s.contractId(), 9_000_001L, today().minusDays(1)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("DBR-07");
    }

    @Test
    void DBR_07_oneAddendumPerApprovedRequest() throws Exception {
        Setup s = activeSetup();
        jdbc.update(INSERT_ADDENDUM, s.contractId(), 9_000_002L, LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> jdbc.update(
                        INSERT_ADDENDUM,
                        s.contractId(),
                        9_000_002L,
                        LocalDate.now().plusDays(1)))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_047_anAddendumCannotBeEditedOrDeleted() throws Exception {
        Setup s = activeSetup();
        createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(2)));
        long addendumId = jdbc.queryForObject(
                "SELECT id FROM contract_addendum WHERE contract_id = ?", Long.class, s.contractId());

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract_addendum SET effective_date = ? WHERE id = ?",
                        today().plusDays(5),
                        addendumId))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FR-047");
        assertThatThrownBy(
                        () -> jdbc.update("UPDATE contract_addendum SET content_after = '{}' WHERE id = ?", addendumId))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM contract_addendum WHERE id = ?", addendumId))
                .isInstanceOf(DataAccessException.class);
        user.set(office.user());
        mvc.perform(put("/api/v1/contracts/" + s.contractId() + "/addenda/" + addendumId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/v1/contracts/" + s.contractId() + "/addenda/" + addendumId)
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void FR_047_anAppliedAddendumCannotGoBackToPending() throws Exception {
        Setup s = activeSetup();
        createAndApprove(request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today()));
        long addendumId = jdbc.queryForObject(
                "SELECT id FROM contract_addendum WHERE contract_id = ?", Long.class, s.contractId());

        assertThat(jdbc.queryForObject("SELECT status FROM contract_addendum WHERE id = ?", String.class, addendumId))
                .isEqualTo("APPLIED");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract_addendum SET status = 'PENDING_EFFECTIVE' WHERE id = ?", addendumId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_07_appliedStatusRequiresAContentVersion() throws Exception {
        Setup s = activeSetup();
        jdbc.update(INSERT_ADDENDUM, s.contractId(), 9_000_003L, LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE contract_addendum SET status = 'APPLIED' WHERE approval_request_id = 9000003"))
                .isInstanceOf(DataAccessException.class);
    }
}
