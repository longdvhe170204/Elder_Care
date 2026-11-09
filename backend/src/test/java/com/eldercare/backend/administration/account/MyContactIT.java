package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationshipsConfig;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.notification.FakeM13Config;
import com.eldercare.backend.support.fake.notification.FakeSmsProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Trang Tài khoản của tôi (api-002 {@code GET /me}, {@code PUT /me/contact}; chủ dự án duyệt 2026-11-09): nhân viên xem
 * và tự sửa số điện thoại, email trên hồ sơ của mình; bắt buộc mật khẩu hiện tại vì số dùng nhận mã tự đặt lại mật khẩu
 * (BR-M15-25); đổi số thì báo tới số cũ; người thân không dùng lệnh này. Một ca (ngân sách IT): luồng chính và các nhánh
 * từ chối.
 */
@AutoConfigureMockMvc
@Import({FakeM15Config.class, FakeFamilyRelationshipsConfig.class, FakeM13Config.class})
class MyContactIT extends MySqlIntegrationTestBase {

    private static final String PASSWORD = "matkhau123";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    FakeStaffProfiles staffProfiles;

    @Autowired
    FakeSmsProvider sms;

    TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        sms.reset();
    }

    @AfterEach
    void tearDown() {
        sms.reset();
    }

    @Test
    void meContact_readsProfile_requiresPassword_validates_notifiesOldNumber() throws Exception {
        TestUsers.TestUser nurse = users.staff(Role.NURSING_STAFF);
        jdbc.update("UPDATE account SET password_hash = ? WHERE id = ?", encoder.encode(PASSWORD), nurse.accountId());
        staffProfiles.phone(nurse.staffId(), "0912000111").email(nurse.staffId(), "cu@vien.vn");
        String token = TestUsers.bearer(users.session(nurse.accountId()));

        mvc.perform(get("/api/v1/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("0912000111"))
                .andExpect(jsonPath("$.email").value("cu@vien.vn"));

        // Sai mật khẩu hiện tại: không đổi gì, không gửi tin
        contact(token, "sai12345", "0987654321", "moi@vien.vn")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"));
        // Sai định dạng: mỗi ô sai một dòng unmetConditions
        contact(token, PASSWORD, "12345", "khong-phai-email")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("CONTACT_INVALID"))
                .andExpect(jsonPath("$.unmetConditions[*].code")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("phoneNumber", "email")));
        assertThat(staffProfiles.phoneNumber(nurse.staffId())).contains("0912000111");
        assertThat(sms.sent()).isEmpty();

        // Đổi số (cho phép dấu cách) và email: lưu dạng chỉ chữ số, báo tới số cũ
        contact(token, PASSWORD, "0987 654 321", "moi@vien.vn")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneNumber").value("0987654321"))
                .andExpect(jsonPath("$.email").value("moi@vien.vn"));
        assertThat(sms.sent())
                .singleElement()
                .satisfies(m -> assertThat(m.phoneNumber()).isEqualTo("0912000111"));

        // Chỉ đổi email, xóa bằng chuỗi rỗng: không gửi tin
        sms.reset();
        contact(token, PASSWORD, "0987654321", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist());
        assertThat(staffProfiles.email(nurse.staffId())).isEmpty();
        assertThat(sms.sent()).isEmpty();

        // Người thân không có hồ sơ nhân viên: 403
        long familyAccount = users.family(TestUsers.nextId()).accountId();
        contact(TestUsers.bearer(users.session(familyAccount)), PASSWORD, "0987654321", null)
                .andExpect(status().isForbidden());
    }

    private ResultActions contact(String token, String password, String phone, String email) throws Exception {
        String body = "{\"currentPassword\":" + quote(password) + ",\"phoneNumber\":" + quote(phone) + ",\"email\":"
                + quote(email) + "}";
        return mvc.perform(put("/api/v1/me/contact")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String quote(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
