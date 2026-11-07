package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/**
 * 012 T014 (FR-020a → FR-020c; US3 kịch bản 9 → 11; research F-06a): mong muốn của người cao tuổi về khách thăm, người
 * đón bất biến, thay thế bằng bản mới; yêu cầu, dòng phiếu trái mong muốn chỉ Hiệu lực khi Quản lý viện duyệt kèm nội
 * dung đã trao đổi; bỏ người đón vẫn áp ngay; người đại diện theo pháp luật không bị chặn; cảnh báo quyền trái tính lúc
 * đọc.
 */
class FR_020_VisitPickupWishIT extends FamilyCommandTestBase {

    long a;
    long rMember;
    long xMember;
    long x;
    String manager;

    @BeforeEach
    void family() throws Exception {
        a = resident(ResidentStatus.IN_RESIDENCE);
        rMember = member("Trần R", "079000000001");
        relate(a, rMember, true, true);
        xMember = fixtures.familyMember("Lê X", "0902000010", "CCCD", "079000000010");
        x = fixtures.relate(xMember, a, 5, false, false);
        manager = staffSession(Role.NURSING_HOME_MANAGER);
    }

    @Test
    void US3_10_requestAgainstWish_waitsForManager_withDiscussionNote() throws Exception {
        long wishId = id(wish(x, "PICKUP", false).andExpect(status().isCreated()));
        String rSession = familySession(rMember);

        JsonNode created =
                json(requestRight(rSession, a, x, "PICKUP_ALLOWED", true).andExpect(status().isCreated()));
        long request = created.get("id").asLong();

        assertThat(created.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(created.get("conflictingWishId").asLong()).isEqualTo(wishId);
        requestCommand(rSession, request, "confirm", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WISH_CONFLICT_REQUIRES_MANAGER"));
        requestCommand(manager, request, "approve", reason("Gia đình cần X đón khám"))
                .andExpect(status().isUnprocessableContent());

        requestCommand(
                        manager,
                        request,
                        "approve",
                        "{\"reason\": \"Gia đình cần X đón khám\", \"discussedWithResident\": \"Bà A đồng ý cho X"
                                + " đón lần này\"}")
                .andExpect(status().isOk());

        assertThat(requestStatus(request)).isEqualTo("EFFECTIVE");
        assertThat(column(x, "pickup_allowed")).isTrue();
    }

    private ResultActions wish(long relationshipId, String type, boolean wanted) throws Exception {
        return postWish(
                "\"relationshipId\": " + relationshipId + ", \"type\": \"" + type + "\", \"wanted\": " + wanted);
    }

    /** Ghi mong muốn bằng phiên Hành chính; nguồn mặc định là người cao tuổi. */
    private ResultActions postWish(String fields) throws Exception {
        String body = fields.contains("\"source\"") ? fields : fields + ", \"source\": {\"type\": \"RESIDENT\"}";
        return post("/api/v1/residents/" + a + "/visit-pickup-wishes", "{" + body + "}");
    }

    private JsonNode wishes() throws Exception {
        return getJson("/api/v1/residents/" + a + "/visit-pickup-wishes");
    }
}
