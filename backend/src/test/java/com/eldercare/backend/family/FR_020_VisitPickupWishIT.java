package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.Set;
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
    void US3_9_wishIsRecorded_rightIsNotTouched_warningIsShown() throws Exception {
        long withPickup = fixtures.relate(
                fixtures.familyMember("Lê Y", "0902000011", "CCCD", "079000000011"),
                a,
                6,
                false,
                false,
                FamilyRight.PICKUP_ALLOWED);

        JsonNode wish = json(wish(withPickup, "PICKUP", false).andExpect(status().isCreated()));

        assertThat(wish.get("status").asString()).isEqualTo("ACTIVE");
        assertThat(wish.get("source").get("type").asString()).isEqualTo("RESIDENT");
        assertThat(column(withPickup, "pickup_allowed")).isTrue();
        JsonNode read = wishes();
        assertThat(read.get("effective"))
                .singleElement()
                .satisfies(w -> assertThat(w.get("id").asLong())
                        .isEqualTo(wish.get("id").asLong()));
        assertThat(read.get("conflictingRights")).singleElement().satisfies(c -> {
            assertThat(c.get("relationshipId").asLong()).isEqualTo(withPickup);
            assertThat(c.get("right").asString()).isEqualTo("PICKUP_ALLOWED");
            assertThat(c.get("wishId").asLong()).isEqualTo(wish.get("id").asLong());
        });
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

    @Test
    void FR_020b_signedFormAtCounter_againstWish_staysPending() throws Exception {
        wish(x, "VISIT", false).andExpect(status().isCreated());

        JsonNode created = json(createRequest(
                        office,
                        a,
                        "\"type\": \"RIGHT\", \"relationshipId\": " + x + ", \"right\": \"REGISTER_VISITS\","
                                + " \"newValue\": true, \"withSignedForm\": true, \"signedByFamilyMemberId\": "
                                + rMember + ", \"evidenceFileId\": " + file())
                .andExpect(status().isCreated()));

        assertThat(created.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(column(x, "register_visits")).isFalse();
    }

    @Test
    void FR_020b_registrationFormLine_againstWish_becomesPending_otherLinesTakeEffect() throws Exception {
        wish(x, "PICKUP", false).andExpect(status().isCreated());

        post(
                        "/api/v1/residents/" + a + "/family-registration-forms",
                        "{\"representativeId\": " + rMember + ", \"scanFileId\": " + file() + ", \"lines\": ["
                                + "{\"relationshipId\": " + x + ", \"right\": \"PICKUP_ALLOWED\"},"
                                + "{\"relationshipId\": " + x + ", \"right\": \"VIEW_COSTS\"}]}")
                .andExpect(status().isCreated());

        assertThat(column(x, "pickup_allowed")).isFalse();
        assertThat(column(x, "view_costs")).isTrue();
        JsonNode requests = getJson("/api/v1/right-change-requests?residentId=" + a);
        assertThat(requests).anySatisfy(rq -> {
            assertThat(rq.get("right").asString()).isEqualTo("PICKUP_ALLOWED");
            assertThat(rq.get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
            assertThat(rq.get("source").asString()).isEqualTo("REGISTRATION_FORM");
        });
    }

    @Test
    void US3_11_removingPickupAgainstWish_appliesAtOnce_newWishSupersedesOld() throws Exception {
        long withPickup = fixtures.relate(
                fixtures.familyMember("Lê Y", "0902000011", "CCCD", "079000000011"),
                a,
                6,
                false,
                false,
                FamilyRight.PICKUP_ALLOWED);
        long wanted = id(wish(withPickup, "PICKUP", true).andExpect(status().isCreated()));

        JsonNode removal = json(requestRight(familySession(rMember), a, withPickup, "PICKUP_ALLOWED", false)
                .andExpect(status().isCreated()));

        assertThat(removal.get("status").asString()).isEqualTo("EFFECTIVE");
        assertThat(column(withPickup, "pickup_allowed")).isFalse();

        long replacement = id(wish(withPickup, "PICKUP", false).andExpect(status().isCreated()));

        JsonNode read = wishes();
        assertThat(read.get("effective"))
                .singleElement()
                .satisfies(w -> assertThat(w.get("id").asLong()).isEqualTo(replacement));
        assertThat(read.get("history")).anySatisfy(w -> {
            assertThat(w.get("id").asLong()).isEqualTo(wanted);
            assertThat(w.get("status").asString()).isEqualTo("SUPERSEDED");
            assertThat(w.get("supersededById").asLong()).isEqualTo(replacement);
        });
    }

    @Test
    void FR_020a_outsider_isMatchedByIdDocument() throws Exception {
        long wishId =
                id(postWish("\"fullName\": \"Lê X\", \"idDocumentNumber\": \"079000000010\", \"type\": \"PICKUP\","
                                + " \"wanted\": false")
                        .andExpect(status().isCreated()));

        JsonNode created = json(requestRight(familySession(rMember), a, x, "PICKUP_ALLOWED", true)
                .andExpect(status().isCreated()));

        assertThat(created.get("conflictingWishId").asLong()).isEqualTo(wishId);
    }

    @Test
    void FR_020c_legalRepresentative_isNotBlocked() throws Exception {
        consents.legalRepresentative(a, rMember, Set.of());
        wish(x, "PICKUP", false).andExpect(status().isCreated());

        JsonNode created = json(requestRight(familySession(rMember), a, x, "PICKUP_ALLOWED", true)
                .andExpect(status().isCreated()));

        assertThat(created.get("status").asString()).isEqualTo("EFFECTIVE");
        assertThat(wishes().get("effective")).hasSize(1);
    }

    @Test
    void FR_020a_wishWithoutPerson_orBadSource_isRejected() throws Exception {
        postWish("\"type\": \"VISIT\", \"wanted\": false").andExpect(status().isUnprocessableContent());
        postWish("\"relationshipId\": " + x + ", \"type\": \"VISIT\", \"wanted\": false, \"source\": {\"type\":"
                        + " \"RESIDENT\", \"familyMemberId\": " + xMember + "}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_OPINION_SOURCE"));
    }

    @Test
    void FR_020a_familyMembers_cannotRecordWishes() throws Exception {
        postAs(
                        familySession(rMember),
                        "/api/v1/residents/" + a + "/visit-pickup-wishes",
                        "{\"relationshipId\": " + x + ", \"type\": \"VISIT\", \"wanted\": false, \"source\":"
                                + " {\"type\": \"RESIDENT\"}}")
                .andExpect(status().isForbidden());
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
