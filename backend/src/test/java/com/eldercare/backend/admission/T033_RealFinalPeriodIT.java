package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 010 T033 (thay bản giả {@code KyCuoiPort} ở test 004, 2026-11-11): không có {@code FakeM02PortsConfig}; lập hồ sơ kết
 * thúc lưu trú (004 FR-062) gọi {@code FinalPeriodService} thật của 010 lập bảng kỳ cuối nháp, mã bảng lưu ở hồ sơ; điều
 * kiện "Chi phí kỳ cuối đã chốt" ({@code END_A_COSTS}) do participant thật của 010 báo chưa thỏa. Hợp đồng, giường, hồ
 * sơ thật ({@link RealCareFixture}).
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(SwitchableUserConfig.class)
class T033_RealFinalPeriodIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    JsonMapper json;

    @Autowired
    java.util.List<com.eldercare.backend.admission.api.spi.PostDeathItemPort> postDeathItems;

    @Autowired
    org.springframework.transaction.support.TransactionTemplate tx;

    @AfterEach
    void restore() {
        user.reset();
    }

    @Test
    void FR_062_endingCreatesRealFinalPeriodDraft_andCostConditionIsReportedByRealParticipant() throws Exception {
        var fixture = new RealCareFixture(jdbc, time);
        TestUser office = fixture.users().staff(Role.OFFICE_STAFF);
        long resident = fixture.residentInBed(fixture.place(), time.now().minus(Duration.ofDays(20)));
        LocalDate expected = time.today().plusDays(10);

        user.set(office.user());
        String created = mvc.perform(post("/api/v1/residency-endings")
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"residentId\":" + resident + ",\"endCase\":\"DISCHARGE\",\"expectedEndDate\":\""
                                + expected + "\",\"requestedBy\":\"Con gái\",\"reason\":\"Gia đình đưa về nhà\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long ending = json.readTree(created).get("id").asLong();

        Long finalRef = jdbc.queryForObject(
                "SELECT final_period_ref FROM residency_ending WHERE kind = 'ENDING' AND id = ?", Long.class, ending);
        assertThat(finalRef).as("mã bảng kỳ cuối do 010 thật trả").isNotNull();
        assertThat(jdbc.queryForObject(
                        "SELECT resident_id FROM cost_statement WHERE id = ? AND kind = 'STATEMENT'",
                        Long.class,
                        finalRef))
                .isEqualTo(resident);

        JsonNode conditions = json.readTree(mvc.perform(get("/api/v1/residency-endings/" + ending + "/conditions"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        JsonNode costs = null;
        for (JsonNode line : conditions.get("conditions"))
            if ("END_A_COSTS".equals(line.get("code").asString())) costs = line;
        assertThat(costs).as(conditions.toString()).isNotNull();
        assertThat(costs.get("status").asString()).isNotIn("MET", "MET_BY_EXCEPTION");

        // Mục việc sau qua đời "Chi phí kỳ cuối" do 010 cài (thay FakePostDeathItem): chưa qua đời, bảng chưa chốt.
        var item = postDeathItems.stream()
                .filter(i -> i.code() == com.eldercare.backend.admission.api.dto.PostDeathItemCode.FINAL_PERIOD_COSTS)
                .findFirst()
                .orElseThrow();
        com.eldercare.backend.admission.api.dto.PostDeathItemStatus itemStatus =
                tx.execute(t -> item.status(resident).status());
        assertThat(itemStatus).isEqualTo(com.eldercare.backend.admission.api.dto.PostDeathItemStatus.NOT_COMPLETED);
    }
}
