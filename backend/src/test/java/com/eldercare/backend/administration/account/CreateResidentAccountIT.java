package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeResidentProfiles;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 002 T083 (tên logic {@code TaoTaiKhoanNguoiCaoTuoiIT}; US8 #1; FR-067, FR-068): Hành chính, Quản lý viện tạo tài khoản
 * cho người cao tuổi Đang lưu trú chưa có tài khoản, kèm xác nhận của chính người cao tuổi; thiếu xác nhận 422; đã có
 * tài khoản, không Đang lưu trú 409; Điều dưỡng không tạo được; vai trò duy nhất AC-16, phạm vi chính mình; buộc đổi
 * mật khẩu lần đầu như bảng chuyển trạng thái của spec.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class CreateResidentAccountIT extends MySqlIntegrationTestBase {

    private static final String CONSENT = "{\"method\":\"REQUESTED\",\"withStaffId\":7}";
    private static final PermissionKey FEEDBACK_CREATE = new PermissionKey(Function.FEEDBACK, Operation.CREATE);

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeResidentProfiles residents;

    @Autowired
    AccountApi accounts;

    @Autowired
    PermissionApi permissions;

    AccountFixtures fixtures;
    String office;
    long resident;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        office = fixtures.session(fixtures.staffAccount("AC-09"));
        resident = AccountFixtures.nextId();
        residents.resident(resident, "Nguyễn Thị Hoa", true, false);
    }

    private ResultActions create(String token, long residentId, String consent) throws Exception {
        String body = "{\"residentId\":" + residentId + ",\"username\":\"nct-" + residentId + "\""
                + (consent == null ? "" : ",\"consent\":" + consent) + "}";
        return mvc.perform(post("/api/v1/resident-accounts")
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    void FR_067_inResidenceWithConsent_createsResidentAccount() throws Exception {
        create(office, resident, CONSENT)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RESIDENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.residentId").value(resident))
                .andExpect(jsonPath("$.roles[0]").value("AC-16"))
                .andExpect(jsonPath("$.fullName").value("Nguyễn Thị Hoa"))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());

        long account = jdbc.queryForObject("SELECT id FROM account WHERE resident_id = ?", Long.class, resident);
        assertThat(jdbc.queryForObject("SELECT must_change_password FROM account WHERE id = ?", Boolean.class, account))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT details FROM audit_log WHERE action = 'ACCOUNT_CREATED' AND entity_id = ?",
                        String.class,
                        String.valueOf(account)))
                .contains("REQUESTED")
                .contains("7");
        assertThat(accounts.fullName(account)).contains("Nguyễn Thị Hoa");
    }

    @Test
    void FR_067_withoutResidentConsent_422() throws Exception {
        create(office, resident, null).andExpect(status().isUnprocessableEntity());
        create(office, resident, "{\"method\":\"REQUESTED\"}").andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account WHERE resident_id = ?", Integer.class, resident))
                .isZero();
    }

    @Test
    void FR_067_alreadyHasAccount_orNotInResidence_409() throws Exception {
        create(office, resident, CONSENT).andExpect(status().isCreated());
        create(office, resident, CONSENT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_HAS_ACCOUNT"));

        long hospitalized = AccountFixtures.nextId();
        residents.resident(hospitalized, "Trần Văn Bình", false, false);
        create(office, hospitalized, CONSENT)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_IN_RESIDENCE"));
    }

    @Test
    void FR_067_nurse_cannotCreate() throws Exception {
        String nurse = fixtures.session(fixtures.staffAccount("AC-04"));

        create(nurse, resident, CONSENT).andExpect(status().isForbidden());
    }

    @Test
    void FR_068_scopeIsOwnResidentOnly() throws Exception {
        create(office, resident, CONSENT).andExpect(status().isCreated());
        long account = jdbc.queryForObject("SELECT id FROM account WHERE resident_id = ?", Long.class, resident);
        ActiveSession user = fixtures.residentUser(account);

        assertThat(permissions
                        .hasPermission(user, FEEDBACK_CREATE, PermissionMatrixIT.resident(resident))
                        .allowed())
                .isTrue();
        assertThat(permissions
                        .hasPermission(user, FEEDBACK_CREATE, PermissionMatrixIT.resident(AccountFixtures.nextId()))
                        .allowed())
                .isFalse();
    }
}
