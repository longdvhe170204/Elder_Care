package com.cambae.backend.m15.parameter;

import static com.cambae.backend.m15.parameter.ParameterFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import com.cambae.backend.support.fake.m15.FakeParameterValueCheck;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 017 T045 phần 000 (017 research E-04): module sở hữu tham số cài {@code ParameterValueCheck} để kiểm giá trị riêng;
 * lệnh đổi giá trị gọi bản cài của đúng mã đó sau phần kiểm chung, trước khi lưu. Từ chối thì 422 kèm lý do của module,
 * giá trị và lịch sử giữ nguyên.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import({ParameterFixtures.class, ParameterValueCheckIT.Checks.class})
class ParameterValueCheckIT extends MySqlIntegrationTestBase {

    // Kiểu bảng, chưa module nào cài kiểm riêng
    private static final String FACILITY = "CFG-M11-11";
    private static final String VALUE = "{\"name\":\"Viện dưỡng lão Bình An\",\"phone\":\"024 3999 8888\"}";

    @TestConfiguration
    static class Checks {

        @Bean
        FakeParameterValueCheck facilityCheck() {
            return new FakeParameterValueCheck(ParameterCode.CFG_M11_11);
        }

        @Bean
        FakeParameterValueCheck otherCheck() {
            return new FakeParameterValueCheck(ParameterCode.CFG_M11_10);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    @Qualifier("facilityCheck")
    FakeParameterValueCheck facilityCheck;

    @Autowired
    @Qualifier("otherCheck")
    FakeParameterValueCheck otherCheck;

    private ParameterClient api;

    @BeforeEach
    void setUp() {
        api = new ParameterClient(mvc);
        user.set(MANAGER);
        facilityCheck.accept();
    }

    @AfterEach
    void restore() throws Exception {
        facilityCheck.accept();
        api.restoreDefault(FACILITY, api.etag(FACILITY), "khôi phục sau test");
    }

    private int historyRows() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM parameter_history WHERE code = ?", Integer.class, FACILITY);
    }

    @Test
    void ownerCheckRejects_is422WithReason_valueUnchanged() throws Exception {
        facilityCheck.reject("Số điện thoại viện không hợp lệ");
        int before = historyRows();

        api.change(FACILITY, api.etag(FACILITY), VALUE, "Nhập thông tin viện")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_VALUE"))
                .andExpect(jsonPath("$.detail").value("Số điện thoại viện không hợp lệ"));

        assertThat(historyRows()).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT current_value FROM parameter WHERE code = ?", String.class, FACILITY))
                .isEqualTo("null");
    }

    @Test
    void ownerCheckAccepts_valueSaved_onlyCheckOfThatCodeCalled() throws Exception {
        int otherCalls = otherCheck.checked().size();

        api.change(FACILITY, api.etag(FACILITY), VALUE, "Nhập thông tin viện").andExpect(status().isOk());

        assertThat(facilityCheck.checked())
                .last()
                .satisfies(v -> assertThat(v.get("name").asString()).isEqualTo("Viện dưỡng lão Bình An"));
        assertThat(otherCheck.checked()).hasSize(otherCalls);
    }

    @Test
    void generalValidationFails_ownerCheckNotCalled() throws Exception {
        int calls = facilityCheck.checked().size();

        api.change(FACILITY, api.etag(FACILITY), "\"không phải bảng\"", "Sai kiểu")
                .andExpect(status().isUnprocessableContent());

        assertThat(facilityCheck.checked()).hasSize(calls);
    }

    @Test
    void restoreDefaultNull_ownerCheckNotCalled() throws Exception {
        api.change(FACILITY, api.etag(FACILITY), VALUE, "Nhập thông tin viện").andExpect(status().isOk());
        facilityCheck.reject("không được gọi khi về Theo cơ sở");

        api.restoreDefault(FACILITY, api.etag(FACILITY), "Về mặc định").andExpect(status().isOk());
    }
}
