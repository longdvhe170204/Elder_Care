package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Gọi lệnh mã QR, đăng nhập bằng mã QR, gỡ thiết bị, khóa tài khoản người cao tuổi cho test của 002 US8. */
record ResidentLogins(MockMvc mvc, JdbcTemplate jdbc) {

    static final String CONSENT = "{\"consent\":{\"method\":\"REQUESTED\",\"withStaffId\":7}}";

    /** Tạo mã theo người cao tuổi của tài khoản, kèm xác nhận (FR-069; T103 gỡ lệnh theo id tài khoản). */
    ResultActions createCode(String token, long account) throws Exception {
        Long resident = jdbc.queryForObject("SELECT resident_id FROM account WHERE id = ?", Long.class, account);
        return mvc.perform(post("/api/v1/resident-accounts/" + resident + "/login-qr-code")
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(CONSENT));
    }

    /** Mã QR mới do người có token tạo; lỗi nếu không tạo được. */
    String code(String token, long account) throws Exception {
        String body = createCode(token, account).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.code");
    }

    ResultActions login(String code, String deviceId) throws Exception {
        return mvc.perform(AccountFixtures.qrLogin(code, deviceId));
    }

    /** Đăng nhập bằng mã QR thành công; trả {@code [token phiên, token thiết bị]}. */
    String[] loggedIn(String staffToken, long account, String deviceId) throws Exception {
        String body = login(code(staffToken, account), deviceId)
                .andReturn()
                .getResponse()
                .getContentAsString();
        return new String[] {JsonPath.read(body, "$.token"), JsonPath.read(body, "$.deviceToken")};
    }

    ResultActions command(String token, long account, String command, String ifMatch, String body) throws Exception {
        return mvc.perform(post("/api/v1/accounts/" + account + "/" + command)
                .header("Authorization", bearer(token))
                .header("If-Match", ifMatch)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
