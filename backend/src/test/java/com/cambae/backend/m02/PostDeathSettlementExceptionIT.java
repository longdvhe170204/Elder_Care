package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ResidencyEndingReadApi;
import com.cambae.backend.m02.api.dto.EndCase;
import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import com.cambae.backend.m02.api.dto.SettlementInfoView;
import com.cambae.backend.m02.api.events.EndingExceptionApproved;
import com.cambae.backend.support.TestUsers.TestUser;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T109 (US8; FR-071a, Q-387): ngoại lệ cho mục quyết toán của danh sách việc sau qua đời. Hành chính hoặc Kế toán lập,
 * Quản lý viện duyệt; dòng ngoại lệ không gắn hồ sơ kết thúc; chỉ mục quyết toán có ngoại lệ; 017 đọc qua {@code
 * ResidencyEndingReadApi}.
 */
@AutoConfigureMockMvc
@WithMockUser
class PostDeathSettlementExceptionIT extends DeathTestBase {

    private static final String BODY = "{\"reason\":\"Gia đình còn nợ, đã làm cam kết\"}";

    @Autowired
    private ResidencyEndingReadApi endings;

    private long deadWithOpenSettlement() throws Exception {
        long residentId = inResidence();
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.NOT_COMPLETED);
        doctorRecordsDeath(residentId, null);
        return residentId;
    }

    private String url(long residentId) {
        return "/api/v1/residents/" + residentId + "/post-death-tasks/request-settlement-exception";
    }

    private long request(TestUser who, long residentId) throws Exception {
        return json(postAs(who, url(residentId), BODY).andExpect(status().isCreated()))
                .get("approvalRequestId")
                .asLong();
    }

    @Test
    void FR_071a_officeRequests_managerApproves_andTheExceptionHasNoEnding() throws Exception {
        long residentId = deadWithOpenSettlement();
        assertThat(endings.hasApprovedSettlementException(residentId)).isFalse();

        long requestId = request(office, residentId);
        assertThat(endings.hasApprovedSettlementException(residentId))
                .as("chưa duyệt")
                .isFalse();
        approve(requestId);

        assertThat(jdbc.queryForMap(
                        "SELECT ending_id, resident_id, condition_code FROM ending_condition_exception WHERE"
                                + " approval_request_id = ?",
                        requestId))
                .containsEntry("ending_id", null)
                .containsEntry("condition_code", "END_H_SETTLEMENT")
                .containsEntry("resident_id", residentId);
        assertThat(endings.hasApprovedSettlementException(residentId)).isTrue();
        assertThat(events.stream(EndingExceptionApproved.class).filter(e -> e.residentId() == residentId))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.residencyEndingId()).isNull();
                    assertThat(e.conditionCode()).isEqualTo("END_H_SETTLEMENT");
                });
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ENDING_EXCEPTION_APPROVED' AND resident_id"
                                + " = ?",
                        Integer.class,
                        residentId))
                .isEqualTo(1);
    }

    @Test
    void FR_071a_accountantMayRequestToo() throws Exception {
        long residentId = deadWithOpenSettlement();

        request(users.staff(Role.ACCOUNTANT), residentId);
    }

    @Test
    void FR_071a_otherRolesCannotRequest() throws Exception {
        long residentId = deadWithOpenSettlement();

        postAs(doctor, url(residentId), BODY).andExpect(status().isForbidden());
        postAs(users.staff(Role.CAREGIVER), url(residentId), BODY).andExpect(status().isForbidden());
    }

    @Test
    void FR_071a_noListMeansNoException() throws Exception {
        long living = inResidence();

        postAs(office, url(living), BODY).andExpect(status().isNotFound());
    }

    @Test
    void FR_071a_aClosedListHasNoException() throws Exception {
        long residentId = inResidence();
        doctorRecordsDeath(residentId, null);
        assertThat(residencyClosed(residentId)).isTrue();

        postAs(office, url(residentId), BODY).andExpect(status().isConflict());
    }

    @Test
    void FR_071a_theExceptionDoesNotApplyIfTheListClosedBeforeTheApproval() throws Exception {
        long residentId = deadWithOpenSettlement();
        long requestId = request(office, residentId);
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.COMPLETED);
        postDeathTasks(office, residentId);

        user.set(manager.user());
        String etag = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/approval-requests/" + requestId))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                        "/api/v1/approval-requests/" + requestId + "/approve")
                                .with(org.springframework.security.test.web.servlet.request
                                        .SecurityMockMvcRequestPostProcessors.csrf())
                                .header("If-Match", etag)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"comment\":\"Đồng ý\"}"))
                .andExpect(status().is4xxClientError());

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM ending_condition_exception WHERE resident_id = ?",
                        Integer.class,
                        residentId))
                .isZero();
    }

    @Test
    void FR_034_settlementInfoForADeadResident_carriesTheDeathDateAndTheContract() throws Exception {
        long residentId = inResidence();
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.NOT_COMPLETED);
        clockAt(d0.plusDays(3), 3, 20);
        Instant deathAt = time.now();
        recordDeath(doctor, residentId, deathDetails(deathAt, null, null));

        Optional<SettlementInfoView> info = endings.settlementInfo(residentId);

        assertThat(info).isPresent();
        assertThat(info.get().endCase()).isEqualTo(EndCase.DEATH);
        assertThat(info.get().deathDate()).isEqualTo(d0.plusDays(3));
        assertThat(info.get().expectedEndDate()).isEqualTo(d0.plusDays(3));
        assertThat(info.get().residenceType()).isEqualTo(ResidenceType.LONG_TERM_RESIDENTIAL);
        assertThat(info.get().contractId()).isNotNull();
        assertThat(info.get().startDate()).isEqualTo(d0);
    }

    @Test
    void FR_034_settlementInfoIsEmptyOnceTheListIsClosed() throws Exception {
        long residentId = inResidence();
        doctorRecordsDeath(residentId, null);

        assertThat(endings.settlementInfo(residentId)).isEmpty();
    }
}
