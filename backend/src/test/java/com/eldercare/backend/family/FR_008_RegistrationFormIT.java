package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.api.events.FamilyRightChanged;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 012 T009 (US1 #9; FR-007 → FR-011, Q-126; quickstart Q1, Q5, Q7): quyền mặc định tắt khi lập quan hệ, trừ FR-009,
 * FR-010; phiếu đăng ký người thân do người đại diện ký (bản scan bắt buộc): mỗi quyền thành một yêu cầu Hiệu lực cách
 * "bản ký" và vẫn qua kiểm tra FR-007, FR-011.
 */
class FR_008_RegistrationFormIT extends FamilyCommandTestBase {

    private static final Set<String> RIGHT_COLUMNS = Set.of(
            "view_health", "view_costs", "receive_urgent", "register_visits", "pickup_allowed", "request_services");

    @Test
    void FR_008_newRelationship_hasEveryRightOff_exceptFr009AndFr010() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long r = relate(a, member("Trần R"), true, true);
        long n = relate(a, member("Trần N"), false, false);

        for (String right : RIGHT_COLUMNS) {
            assertThat(column(n, right)).as(right).isFalse();
        }
        assertThat(column(r, "receive_urgent")).isTrue();
        assertThat(column(r, "request_services")).isTrue();
        assertThat(column(r, "view_costs")).isFalse();
    }

    @Test
    void FR_008_eachRightOnTheForm_becomesAnEffectiveSignedFormRequest() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long rMember = member("Trần R");
        long r = relate(a, rMember, true, true);
        long n = relate(a, member("Trần N"), false, false);
        consents.active(a, Set.of(rMember), true);
        long scan = file();

        long form =
                id(form(a, rMember, scan, Map.of(n, FamilyRight.REGISTER_VISITS), Map.of(r, FamilyRight.VIEW_HEALTH))
                        .andExpect(status().isCreated()));

        assertThat(column(n, "register_visits")).isTrue();
        assertThat(column(r, "view_health")).isTrue();
        assertThat(history.valueAt(n, RelationshipAttribute.REGISTER_VISITS, time.now()))
                .isTrue();
        List<Map<String, Object>> requests = jdbc.queryForList(
                "SELECT status, content->>'$.relationshipId' AS rel, content->>'$.right' AS r,"
                        + " content->>'$.confirmationMethod' AS method, content->>'$.source' AS src,"
                        + " content->>'$.formId' AS form FROM simple_record"
                        + " WHERE kind = 'M10.RIGHT_CHANGE_REQUEST' AND resident_id = ? ORDER BY id",
                a);
        assertThat(requests).hasSize(2).allSatisfy(row -> {
            assertThat(row.get("status")).isEqualTo("EFFECTIVE");
            assertThat(row.get("method")).isEqualTo("SIGNED_FORM");
            assertThat(row.get("src")).isEqualTo("REGISTRATION_FORM");
            assertThat(row.get("form")).isEqualTo(String.valueOf(form));
        });
        assertThat(requests)
                .extracting(row -> row.get("rel") + ":" + row.get("r"))
                .containsExactlyInAnyOrder(n + ":REGISTER_VISITS", r + ":VIEW_HEALTH");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_LENGTH(content->'$.lines') FROM record_event"
                                + " WHERE kind = 'M10.FAMILY_REGISTRATION_FORM' AND id = ?",
                        Integer.class,
                        form))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT owner_type FROM file WHERE id = ?", String.class, scan))
                .isEqualTo("M10.FAMILY_REGISTRATION_FORM");
        assertThat(events.stream(FamilyRightChanged.class))
                .anyMatch(e -> e.relationshipId() == n && e.right() == FamilyRight.REGISTER_VISITS && e.value());
    }

    @Test
    void FR_011_viewHealthOutsideTheConsent_isRejected_andNothingIsApplied() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long rMember = member("Lê R");
        relate(a, rMember, true, true);
        long n = relate(a, member("Lê N"), false, false);
        consents.active(a, Set.of(rMember), true);

        form(a, rMember, file(), Map.of(n, FamilyRight.REGISTER_VISITS), Map.of(n, FamilyRight.VIEW_HEALTH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONSENT_NOT_INCLUDED"));

        assertThat(column(n, "view_health")).isFalse();
        assertThat(column(n, "register_visits")).isFalse();
    }

    @Test
    void FR_007_pickupAllowed_needsAnIdDocument() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long rMember = member("Phạm R");
        relate(a, rMember, true, true);
        long m = relate(a, member("Phạm M"), false, false);
        long x = relate(a, member("Phạm X", "079000000002"), false, false);

        form(a, rMember, file(), Map.of(m, FamilyRight.PICKUP_ALLOWED), Map.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ID_DOCUMENT_REQUIRED"));
        form(a, rMember, file(), Map.of(x, FamilyRight.PICKUP_ALLOWED), Map.of())
                .andExpect(status().isCreated());

        assertThat(column(m, "pickup_allowed")).isFalse();
        assertThat(column(x, "pickup_allowed")).isTrue();
    }

    @Test
    void FR_008_theFormIsSignedByARepresentative() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        relate(a, member("Võ R"), true, true);
        long nMember = member("Võ N");
        long n = relate(a, nMember, false, false);

        form(a, nMember, file(), Map.of(n, FamilyRight.REGISTER_VISITS), Map.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_NOT_ALLOWED"));
        assertThat(column(n, "register_visits")).isFalse();
    }

    @Test
    void FR_010_requestServices_onlyForARepresentative() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long rMember = member("Đỗ R");
        relate(a, rMember, true, true);
        long n = relate(a, member("Đỗ N"), false, false);

        form(a, rMember, file(), Map.of(n, FamilyRight.REQUEST_SERVICES), Map.of())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_NOT_ALLOWED"));
    }

    private ResultActions form(
            long residentId,
            long representativeId,
            long scanFileId,
            Map<Long, FamilyRight> first,
            Map<Long, FamilyRight> second)
            throws Exception {
        StringBuilder lines = new StringBuilder();
        for (Map<Long, FamilyRight> part : List.of(first, second)) {
            part.forEach((relationshipId, right) -> lines.append(lines.isEmpty() ? "" : ", ")
                    .append("{\"relationshipId\": ")
                    .append(relationshipId)
                    .append(", \"right\": \"")
                    .append(right.name())
                    .append("\"}"));
        }
        return post(
                "/api/v1/residents/" + residentId + "/family-registration-forms",
                "{\"representativeId\": " + representativeId + ", \"scanFileId\": " + scanFileId + ", \"lines\": ["
                        + lines + "]}");
    }
}
