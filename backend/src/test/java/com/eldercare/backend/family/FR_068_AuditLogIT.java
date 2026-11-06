package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 012 T040, FR-068: lệnh trên dữ liệu nhóm 2 ghi nhật ký với người thực hiện, thời điểm, lý do, giá trị trước/sau (000).
 * Phần "tại một thời điểm ai được phép đón" kiểm ở {@code T003_FamilyRightsApiIT}, {@code BR_M10_07_RightChangeRequestIT}.
 */
class FR_068_AuditLogIT extends FamilyCommandTestBase {

    private List<Map<String, Object>> auditOf(String entityType, long entityId) {
        return jdbc.queryForList(
                "SELECT action, actor_type, actor_id, occurred_at, reason, CAST(data_before AS CHAR) AS data_before,"
                        + " CAST(data_after AS CHAR) AS data_after FROM audit_log WHERE entity_type = ? AND entity_id = ?"
                        + " ORDER BY occurred_at, id",
                entityType,
                String.valueOf(entityId));
    }

    @Test
    void FR_068_updateFamilyMember_logsActorTimeBeforeAndAfter() throws Exception {
        long member = member("Lý Văn Tư");
        String etag = mvc.perform(get("/api/v1/family-members/" + member).header("Authorization", office))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        mvc.perform(put("/api/v1/family-members/" + member)
                        .header("Authorization", office)
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"Lý Văn Tư\", \"phoneNumber\": \"0912345678\"}"))
                .andExpect(status().isOk());

        Map<String, Object> updated = auditOf("M10.FAMILY_MEMBER", member).getLast();
        assertThat(updated.get("action")).isEqualTo("FAMILY_MEMBER_UPDATED");
        assertThat(updated.get("actor_type")).isEqualTo("USER");
        assertThat(updated.get("actor_id")).isNotNull();
        assertThat(updated.get("occurred_at")).isNotNull();
        assertThat((String) updated.get("data_before")).doesNotContain("0912345678");
        assertThat((String) updated.get("data_after")).contains("0912345678");
    }

    @Test
    void FR_068_changePrimaryContact_logsReasonAndRoleBeforeAndAfter() throws Exception {
        long a = resident(ResidentStatus.IN_RESIDENCE);
        long r = relate(a, member("Lý R"), true, true);
        long m = relate(a, member("Lý M"), false, false);

        command(m, "change-primary-contact", reason("R đi công tác dài ngày")).andExpect(status().isOk());

        // Một dòng trên quan hệ nhận vai trò; trước/sau là quan hệ đang giữ vai trò liên hệ chính
        Map<String, Object> last = auditOf("M10.FAMILY_RELATIONSHIP", m).getLast();
        assertThat(last.get("action")).isEqualTo("FAMILY_PRIMARY_CONTACT_CHANGED");
        assertThat(last.get("actor_type")).isEqualTo("USER");
        assertThat(last.get("actor_id")).isNotNull();
        assertThat(last.get("occurred_at")).isNotNull();
        assertThat(last.get("reason")).isEqualTo("R đi công tác dài ngày");
        assertThat((String) last.get("data_before")).contains(String.valueOf(r));
        assertThat((String) last.get("data_after")).contains(String.valueOf(m));
    }
}
