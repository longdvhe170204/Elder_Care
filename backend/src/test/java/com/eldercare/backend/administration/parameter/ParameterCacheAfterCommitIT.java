package com.eldercare.backend.administration.parameter;

import static com.eldercare.backend.administration.parameter.ParameterFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * US4 #3; SC-004, FR-016, NFR-12 (000 T077): sau khi lệnh đổi commit, lần đọc kế tiếp của quy tắc dùng giá trị mới,
 * không khởi động lại; lần đọc trước đó đã nằm trong bộ đệm.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(ParameterFixtures.class)
class ParameterCacheAfterCommitIT extends MySqlIntegrationTestBase {

    private static final String ALERT = "CFG-M05-01";

    @Autowired
    ParameterApi parameters;

    @Autowired
    MockMvc mvc;

    @Autowired
    SwitchableUser user;

    private ParameterClient api;

    @BeforeEach
    void setUp() {
        api = new ParameterClient(mvc);
        user.set(MANAGER);
    }

    @AfterEach
    void restore() throws Exception {
        api.restoreDefault(ALERT, api.etag(ALERT), "khôi phục sau test");
    }

    @Test
    void SC_004_ruleReadsNewValueRightAfterCommit_withoutRestart() throws Exception {
        assertThat(parameters.read(ParameterCode.CFG_M05_01).asDuration()).isEqualTo(Duration.ofMinutes(15));

        api.change(ALERT, api.etag(ALERT), "{\"value\":25,\"unit\":\"MINUTES\"}", "Đổi để kiểm bộ đệm")
                .andExpect(status().isOk());

        assertThat(parameters.read(ParameterCode.CFG_M05_01).asDuration()).isEqualTo(Duration.ofMinutes(25));
    }

    @Test
    void FR_015_rejectedChange_leavesCachedValue() throws Exception {
        assertThat(parameters.read(ParameterCode.CFG_M05_01).asDuration()).isEqualTo(Duration.ofMinutes(15));

        api.change(ALERT, api.etag(ALERT), "{\"value\":-5,\"unit\":\"MINUTES\"}", "Sai")
                .andExpect(status().isUnprocessableContent());

        assertThat(parameters.read(ParameterCode.CFG_M05_01).asDuration()).isEqualTo(Duration.ofMinutes(15));
    }
}
