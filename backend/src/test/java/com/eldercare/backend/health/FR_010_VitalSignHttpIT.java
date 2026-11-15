package com.eldercare.backend.health;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-010, FR-011 (quickstart 007 Q1 → Q5, lỗi B1 của script nghiệm thu): ghi, xem chỉ số qua HTTP với quyền và phạm vi
 * 002 thật. Người được phân công người cao tuổi ghi, xem được; người không được phân công không thấy người đó.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
@Transactional
class FR_010_VitalSignHttpIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    CatalogApi catalogs;

    TestUsers users;
    long resident;
    long indicator;

    @BeforeEach
    void prepare() {
        users = new TestUsers(jdbc, time);
        resident = TestUsers.nextId();
        indicator = catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "SPO2_HTTP_" + resident, "SpO2", null, 0, Map.of("unit", "%", "vitalSign", true)))
                .id();
        VitalSignTestThresholds.seed(jdbc, indicator);
    }

    @Test
    void FR_010_assignedStaffRecordsAndViewsVitalSigns_othersDoNotSeeResident() throws Exception {
        TestUser caregiver = assigned(Role.CAREGIVER);
        TestUser nurse = assigned(Role.NURSING_STAFF);

        record(caregiver)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.indicatorId").value(indicator));
        mvc.perform(as(nurse, MockMvcRequestBuilders.get("/api/v1/residents/{id}/vital-signs", resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        TestUser outsider = users.staff(Role.CAREGIVER);
        record(outsider).andExpect(status().isNotFound());
        mvc.perform(as(outsider, MockMvcRequestBuilders.get("/api/v1/residents/{id}/vital-signs", resident)))
                .andExpect(status().isNotFound());
    }

    private TestUser assigned(Role role) {
        TestUser user = users.staff(role);
        assignments.assign(
                user.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        resident,
                        TestUsers.nextId(),
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(7))));
        return user;
    }

    private ResultActions record(TestUser as) throws Exception {
        String body = "{\"indicatorId\":" + indicator + ",\"values\":{\"VALUE\":97},\"measuredAt\":\""
                + time.now().minus(Duration.ofMinutes(5)) + "\"}";
        return mvc.perform(as(as, MockMvcRequestBuilders.post("/api/v1/residents/{id}/vital-signs", resident))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private MockHttpServletRequestBuilder as(TestUser user, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", TestUsers.bearer(users.session(user.accountId())));
    }
}
