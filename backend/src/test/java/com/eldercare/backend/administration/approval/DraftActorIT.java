package com.eldercare.backend.administration.approval;

import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_A;
import static com.eldercare.backend.administration.approval.ApprovalFixtures.OFFICE_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Người thao tác trên Nháp (FR-033; bổ sung theo plan 004 FR-039): mặc định chỉ người yêu cầu sửa, gửi duyệt; loại yêu
 * cầu cho thêm vai trò khác thì người đó sửa, gửi được, người yêu cầu giữ nguyên, chỉ người yêu cầu được Hủy.
 */
class DraftActorIT extends ApprovalTestBase {

    @Autowired
    ApprovalRequestApi requests;

    @Autowired
    TransactionTemplate transaction;

    private static final String EDIT = "{\"content\":{\"key\":\"đã hoàn thiện\"},\"reason\":\"Hoàn thiện yêu cầu\"}";

    @Test
    void FR_033_byDefault_onlyTheRequesterEditsAndSubmitsTheDraft() throws Exception {
        user.set(OFFICE_A);
        long id = api.create(time.today().plusDays(30), false).get("id").asLong();

        user.set(OFFICE_B);
        mvc.perform(get(ApprovalClient.URL + "/" + id))
                .andExpect(jsonPath("$.allowedCommands[?(@ == 'edit-content')]").doesNotExist());
        api.command(id, "edit-content", api.etag(id), EDIT).andExpect(status().isForbidden());
        api.submit(id).andExpect(status().isForbidden());
    }

    @Test
    void FR_039_typeAllowsAnotherRole_editsAndSubmits_requesterUnchanged() throws Exception {
        control.draftActorRoles = Set.of(Role.OFFICE_STAFF);
        user.set(OFFICE_A);
        long id = api.create(time.today().plusDays(30), false).get("id").asLong();

        user.set(OFFICE_B);
        mvc.perform(get(ApprovalClient.URL + "/" + id))
                .andExpect(jsonPath("$.allowedCommands[?(@ == 'edit-content')]").exists())
                .andExpect(jsonPath("$.allowedCommands[?(@ == 'submit')]").exists())
                .andExpect(jsonPath("$.allowedCommands[?(@ == 'cancel')]").doesNotExist());
        api.command(id, "edit-content", api.etag(id), EDIT).andExpect(status().isOk());
        api.cancel(id, "Không cần nữa").andExpect(status().isForbidden());
        api.submit(id).andExpect(status().isOk());

        assertThat(read(id).get("status").asString()).isEqualTo("PENDING_APPROVAL");
        assertThat(read(id).get("content").get("key").asString()).isEqualTo("đã hoàn thiện");
        assertThat(jdbc.queryForObject("SELECT requester_id FROM approval_request WHERE id = ?", Long.class, id))
                .isEqualTo(OFFICE_A.accountId());
    }

    /** 004 T043: PUT /residency-change-requests/{id} sửa Nháp qua api, trong transaction của lệnh 004. */
    @Test
    void FR_039_editDraft_throughApi_sameChecksAsHttp() throws Exception {
        user.set(OFFICE_A);
        long id = api.create(time.today().plusDays(30), false).get("id").asLong();
        String etag = api.etag(id);
        var content = json.readTree("{\"key\":\"qua api\"}");
        Reason reason = new Reason("Hoàn thiện yêu cầu");

        assertThatThrownBy(() -> requests.editDraft(id, etag, content, null, reason))
                .isInstanceOf(IllegalTransactionStateException.class);
        user.set(OFFICE_B);
        assertThatThrownBy(() ->
                        transaction.executeWithoutResult(s -> requests.editDraft(id, etag, content, null, reason)))
                .isInstanceOf(PermissionDeniedException.class);
        control.draftActorRoles = Set.of(Role.OFFICE_STAFF);
        assertThatThrownBy(
                        () -> transaction.executeWithoutResult(s -> requests.editDraft(id, etag, content, null, null)))
                .isInstanceOf(ReasonRequiredException.class);
        transaction.executeWithoutResult(s -> requests.editDraft(id, etag, content, null, reason));

        assertThat(read(id).get("content").get("key").asString()).isEqualTo("qua api");
        assertThat(read(id).get("requestedEffectiveDate").asString())
                .isEqualTo(time.today().plusDays(30).toString());
    }
}
