package com.cambae.backend.m15.parameter;

import static com.cambae.backend.m15.parameter.ParameterFixtures.MANAGER;
import static com.cambae.backend.m15.parameter.ParameterFixtures.NURSE;
import static com.cambae.backend.m15.parameter.ParameterFixtures.OFFICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * US4 #6; BR-M15-04, FR-013 (000 T076; quickstart K4): chỉ Quản lý viện xem và đổi tham số (Phụ lục 27 cột C, chú
 * thích ¹²); vai trò khác bị từ chối, giá trị giữ nguyên.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(ParameterFixtures.class)
class ParameterPermissionIT extends MySqlIntegrationTestBase {

    private static final String ALERT = "CFG-M05-01";

    @Autowired
    MockMvc mvc;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void resetUser() {
        user.set(MANAGER);
    }

    @Test
    void US4_6_nonManager_cannotChange_norBrowseParameters() throws Exception {
        user.set(MANAGER);
        ParameterClient api = new ParameterClient(mvc);
        String etag = api.etag(ALERT);
        int history =
                jdbc.queryForObject("SELECT COUNT(*) FROM parameter_history WHERE code = ?", Integer.class, ALERT);

        for (CurrentUser other : List.of(NURSE, OFFICE)) {
            user.set(other);
            api.change(ALERT, etag, "{\"value\":20,\"unit\":\"MINUTES\"}", "Thử đổi")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("PERMISSION_DENIED"));
            mvc.perform(get("/api/v1/parameters")).andExpect(status().isForbidden());
            mvc.perform(get(ParameterClient.URL + ALERT)).andExpect(status().isNotFound());
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parameter_history WHERE code = ?", Integer.class, ALERT))
                .isEqualTo(history);
    }

    @Test
    void FR_011_unknownCode_isNotFound() throws Exception {
        user.set(MANAGER);
        mvc.perform(get(ParameterClient.URL + "CFG-M99-99")).andExpect(status().isNotFound());
    }
}
