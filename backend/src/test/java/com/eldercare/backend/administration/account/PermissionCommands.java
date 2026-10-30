package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Gọi lệnh phân quyền của 002 US7 và dọn thu hẹp theo vai trò sau test (bảng dùng chung giữa các test). */
record PermissionCommands(MockMvc mvc, JdbcTemplate jdbc) {

    ResultActions account(String token, long account, String command, String ifMatch, String body) throws Exception {
        return send(token, "/api/v1/accounts/" + account + "/" + command, ifMatch, body);
    }

    ResultActions role(String token, String role, String command, String ifMatch, String body) throws Exception {
        return send(token, "/api/v1/roles/" + role + "/" + command, ifMatch, body);
    }

    String accountVersion(long account) {
        return "\"" + jdbc.queryForObject("SELECT permission_version FROM account WHERE id = ?", Long.class, account)
                + "\"";
    }

    String roleVersion(String role) {
        return "\""
                + jdbc.queryForObject(
                        "SELECT CAST(attributes->'$.permissionVersion' AS SIGNED) FROM catalog_item"
                                + " WHERE catalog_type = 'M15.ROLE' AND code = ?",
                        Long.class,
                        role)
                + "\"";
    }

    /** Đóng mọi thu hẹp theo vai trò còn hiệu lực, tăng phiên bản để bộ đệm quyền của test khác tính lại. */
    void closeRoleRestrictions() {
        jdbc.update("UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.permissionVersion',"
                + " CAST(attributes->'$.permissionVersion' AS SIGNED) + 1), version = version + 1"
                + " WHERE catalog_type = 'M15.ROLE' AND code IN (SELECT role FROM permission_assignment"
                + " WHERE kind = 'RESTRICTION' AND applies_to = 'ROLE' AND effective_to IS NULL)");
        jdbc.update("UPDATE permission_assignment SET effective_to = effective_from"
                + " WHERE kind = 'RESTRICTION' AND applies_to = 'ROLE' AND effective_to IS NULL");
    }

    static String restrict(String function, String operation, String kind) {
        return "{\"function\":\"" + function + "\",\"operation\":\"" + operation + "\",\"kind\":\"" + kind
                + "\",\"reason\":\"Điều chỉnh theo mô hình vận hành\"}";
    }

    static String restore(long restrictionId) {
        return "{\"restrictionId\":" + restrictionId + ",\"reason\":\"Khôi phục về mức ma trận\"}";
    }

    static final String REASON = "{\"reason\":\"Có chứng chỉ chuyên môn\"}";

    private ResultActions send(String token, String path, String ifMatch, String body) throws Exception {
        return mvc.perform(post(path)
                .header("Authorization", bearer(token))
                .header("If-Match", ifMatch)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
