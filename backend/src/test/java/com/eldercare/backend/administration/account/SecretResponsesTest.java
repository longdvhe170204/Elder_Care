package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.internal.auth.service.ResidentAccountService.LoginCode;
import com.eldercare.backend.administration.account.web.response.AccountWithTemporaryPassword;
import com.eldercare.backend.administration.account.web.response.TemporaryPassword;
import com.eldercare.backend.common.web.SecretResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

/** NFR-08: mọi kiểu phản hồi chứa mật khẩu tạm, mã QR đều được đánh dấu để không lưu nguyên văn. */
class SecretResponsesTest {

    @Test
    void NFR_08_temporaryPasswordAndLoginCode_areSecretResponses() {
        assertThat(List.of(AccountWithTemporaryPassword.class, TemporaryPassword.class, LoginCode.class))
                .allMatch(SecretResponse.class::isAssignableFrom);
    }
}
