package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.nutrition.api.DietApi;
import com.eldercare.backend.nutrition.api.events.DietAssignmentTookEffect;
import com.eldercare.backend.nutrition.scheduling.M08MinuteJob;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 011 T012 (US1, BR-M08-03, FR-010 → FR-014, FR-017, FR-001, FR-002; quickstart Q1 → Q5): bản gán Đề xuất → Áp dụng
 * (Hiệu lực ngay hoặc Chờ hiệu lực) → Ngừng; lý do chuyên môn khi thuộc FR-013; một bản chờ và một bản Hiệu lực mỗi
 * người (cột sinh UNIQUE); nội dung khóa bằng trigger; bếp không xem bản gán.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import(SwitchableUserConfig.class)
class BR_33_DietAssignmentIT extends MySqlIntegrationTestBase {

    private static final String ASSIGNMENTS = "/api/v1/diet-assignments/";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser user;

    @Autowired
    DietApi dietApi;

    @Autowired
    M08MinuteJob minuteJob;

    @Autowired
    ApplicationEvents events;

    NutritionCatalogFixture f;
    long regular;
    long diabetes;
    long resident;
    long safe;

    @BeforeEach
    void setUp() throws Exception {
        f = new NutritionCatalogFixture(mvc, jdbc, time, user);
        long base = f.baseDiet();
        safe = f.activeDish(List.of(), List.of(base));
        regular = f.diet(false, List.of(), safe);
        diabetes = f.treatmentDiet(List.of(), safe);
        resident = f.resident("IN_RESIDENCE");
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private Instant now() {
        return time.now();
    }

    @Test
    void BR_33_treatmentDietWithoutProfessionalReason_blocked_thenAppliedWithReason() throws Exception {
        long old = f.activeAssignment(resident, regular, "REGULAR", List.of(), now());

        f.propose(resident, NutritionCatalogFixture.assignmentBody(diabetes, "SOFT", List.of(), now()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.professionalReasonRequired").value(true));
        long proposal = f.latestAssignment(resident);

        f.apply(proposal, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROFESSIONAL_REASON_REQUIRED"));
        assertThat(f.assignmentStatus(proposal)).isEqualTo("PROPOSED");
        assertThat(f.assignmentStatus(old)).isEqualTo("ACTIVE");

        f.apply(proposal, "Đường huyết cao kéo dài").andExpect(status().isOk());

        assertThat(f.assignmentStatus(proposal)).isEqualTo("ACTIVE");
        assertThat(f.assignmentStatus(old)).isEqualTo("DISCONTINUED");
        assertThat(jdbc.queryForObject(
                        "SELECT professional_reason FROM diet_assignment WHERE id = ?", String.class, proposal))
                .isEqualTo("Đường huyết cao kéo dài");
        assertThat(events.stream(DietAssignmentTookEffect.class).filter(e -> e.assignmentId() == proposal))
                .hasSize(1);
        assertThat(dietApi.current(resident).orElseThrow().dietId()).isEqualTo(diabetes);
    }
}
