package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 013 T022 (US5; BR-M12-08, FR-027a, Q-152, Q-157, Q-159, SC-010; quickstart Q33 → Q36): đề nghị "Xử lý đồ không người
 * nhận" là yêu cầu phê duyệt của 000: chặn trước CFG-M12-03, bằng chứng liên hệ, biên bản, tiền mặt chỉ chuyển cơ quan
 * có thẩm quyền, đồ Đang sử dụng phải Thu lại, không tự duyệt (Quản lý viện duyệt), áp dụng ghi bản ghi Xử lý và loại
 * đồ đã đổi trạng thái.
 */
class BR_M12_08_DisposalIT extends HandoverTestBase {

    private static final String TYPE = "M12.UNCLAIMED_BELONGING_DISPOSAL";

    private void endResident(long resident, int daysAgo) {
        jdbc.update(
                "UPDATE resident SET status = 'RESIDENCY_ENDED', status_since = ? WHERE id = ?",
                LocalDateTime.ofInstant(time.now().minus(Duration.ofDays(daysAgo)), ZoneOffset.UTC),
                resident);
    }

    private long representative(long resident) {
        long member = TestUsers.nextId();
        family.representative(member, resident);
        return member;
    }

    private String attempts(long member, int firstDaysAgo, int secondDaysAgo) {
        Instant now = time.now();
        return "[{\"familyMemberId\":" + member + ",\"channel\":\"PHONE_CALL\",\"at\":\""
                + now.minus(Duration.ofDays(firstDaysAgo)) + "\",\"result\":\"Không nghe máy\"},"
                + "{\"familyMemberId\":" + member + ",\"channel\":\"TEXT_MESSAGE\",\"at\":\""
                + now.minus(Duration.ofDays(secondDaysAgo)) + "\",\"result\":\"Đã nhắn\"}]";
    }

    private ResultActions create(
            String token, long resident, String ids, String method, String attempts, boolean minutes) throws Exception {
        String content = "{\"residentId\":" + resident + ",\"belongingIds\":[" + ids + "],\"method\":\"" + method
                + "\",\"receivingUnit\":\"UBND phường\","
                + (minutes ? "\"minutesFileId\":" + photo() + "," : "")
                + "\"photoFileIds\":[" + photo() + "],\"contactAttempts\":" + attempts + "}";
        return mvc.perform(post("/api/v1/approval-requests")
                .header("Authorization", token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"" + TYPE + "\",\"content\":" + content + ",\"requestedEffectiveDate\":\""
                        + time.today() + "\",\"reason\":\"Không ai nhận\",\"submitImmediately\":true}"));
    }

    private long createdId(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(
                        result.andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.id"))
                .longValue();
    }

    private ResultActions approve(String token, long requestId) throws Exception {
        String etag = mvc.perform(get("/api/v1/approval-requests/" + requestId).header("Authorization", manager))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        return mvc.perform(post("/api/v1/approval-requests/" + requestId + "/approve")
                .header("Authorization", token)
                .header("If-Match", etag)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"Duyệt\"}"));
    }

    @Test
    void Q34_approvedRequestMovesTheItemToDisposed_withDisposalRecord_andManagerOnlyApproves() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long clothes = intake(resident, "CLOTHING", "\"quantity\": 1", location());
        endResident(resident, 100);
        long member = representative(resident);

        long request =
                createdId(create(office, resident, String.valueOf(clothes), "SELL", attempts(member, 20, 10), true));
        approve(nurse, request).andExpect(status().isForbidden());
        assertThat(statusOf(clothes)).isEqualTo("HELD");

        approve(manager, request).andExpect(status().isOk());

        assertThat(statusOf(clothes)).isEqualTo("DISPOSED");
        var row = jdbc.queryForMap(
                "SELECT kind, approval_request_id, receiving_unit FROM belonging_handover WHERE id = ?",
                latestHandover(clothes));
        assertThat(row)
                .containsEntry("kind", "DISPOSAL")
                .containsEntry("approval_request_id", request)
                .containsEntry("receiving_unit", "UBND phường");
    }
}
