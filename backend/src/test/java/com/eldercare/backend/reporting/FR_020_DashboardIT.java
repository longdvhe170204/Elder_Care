package com.eldercare.backend.reporting;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.ExpiringFacilityLicense;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeFacilityLicenses;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * FR-010, FR-020, FR-021, US1 #2, #6 (016 T009; research O-06): dashboard chỉ có khối vai trò được thấy trong phạm vi hiện
 * hành; giấy phép cơ sở chỉ Quản lý viện; không có phạm vi thì "Hiện không có phạm vi phân công"; số liệu đệm 20 giây
 * theo phạm vi; ETag cho làm mới 30 giây (304 khi không đổi); vai trò không có dòng "Dashboard, báo cáo" bị từ chối.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class FR_020_DashboardIT extends MySqlIntegrationTestBase {

    private static final String URL = "/api/v1/dashboard";
    private static final String LICENSES = "$.blocks[?(@.group == 'FACILITY_LICENSES')]";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeFacilityLicenses licenses;

    TestUsers users;

    // mỗi test một mốc cách nhau 1 phút để bộ đệm 20 giây của test trước không còn hiệu lực
    private static int step;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        clock.setTo(time.now().plus(Duration.ofMinutes(++step)));
    }

    @AfterEach
    void reset() {
        licenses.reset();
        clock.reset();
    }

    private ResultActions dashboard(TestUser u) throws Exception {
        return mvc.perform(get(URL).header("Authorization", TestUsers.bearer(users.session(u.accountId()))));
    }

    @Test
    void US1_6_manager_seesFacilityLicenses_floorManagerDoesNot() throws Exception {
        licenses.expiring(new ExpiringFacilityLicense(
                TestUsers.nextId(), "Giấy phép hoạt động", time.today().plusDays(45)));
        TestUser manager = users.staff(Role.NURSING_HOME_MANAGER);
        TestUser floorManager = users.staff(Role.FLOOR_MANAGER);
        assignments.assign(
                floorManager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        TestUsers.nextId(),
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));

        dashboard(manager)
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath(LICENSES + ".values[0].metricCode").value(hasItem("LICENSE.FACILITY")))
                .andExpect(jsonPath(LICENSES + ".values[0].value").value(hasItem(1)))
                .andExpect(jsonPath(LICENSES + ".status").value(hasItem("COMPLETE")))
                .andExpect(jsonPath("$.blocks[*].group").value(hasItem("INCURRED_COSTS")));
        dashboard(floorManager)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[*].group").value(not(hasItem("FACILITY_LICENSES"))))
                .andExpect(jsonPath("$.blocks[*].group").value(not(hasItem("INCURRED_COSTS"))))
                .andExpect(jsonPath("$.blocks[*].group").value(hasItem("CARE_TASKS")));
    }
}
