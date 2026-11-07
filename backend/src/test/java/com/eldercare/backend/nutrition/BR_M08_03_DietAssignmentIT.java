package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.nutrition.api.DietApi;
import com.eldercare.backend.nutrition.api.events.DietAssignmentTookEffect;
import com.eldercare.backend.nutrition.scheduling.M08MinuteJob;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
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
@Import(BR_M08_03_DietAssignmentIT.Users.class)
class BR_M08_03_DietAssignmentIT extends MySqlIntegrationTestBase {

    private static final String ASSIGNMENTS = "/api/v1/diet-assignments/";

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser nutritionAssignmentUser() {
            return new SwitchableUser();
        }
    }

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
    void BR_M08_03_treatmentDietWithoutProfessionalReason_blocked_thenAppliedWithReason() throws Exception {
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

    @Test
    void BR_M08_03_nonTreatmentDiet_activeImmediatelyWithoutReason() throws Exception {
        long old = f.activeAssignment(resident, regular, "REGULAR", List.of(), now());
        long vegetarian = f.diet(false, List.of(), safe);

        f.propose(resident, NutritionCatalogFixture.assignmentBody(vegetarian, "REGULAR", List.of(), now()))
                .andExpect(jsonPath("$.professionalReasonRequired").value(false));
        long proposal = f.latestAssignment(resident);
        f.apply(proposal, null).andExpect(status().isOk());

        assertThat(f.assignmentStatus(proposal)).isEqualTo("ACTIVE");
        assertThat(f.assignmentStatus(old)).isEqualTo("DISCONTINUED");
    }

    @Test
    void FR_013_firstAssignmentComparedWithDefaultDiet() throws Exception {
        f.diet(true, List.of(), safe);

        f.propose(resident, NutritionCatalogFixture.assignmentBody(diabetes, "REGULAR", List.of(), now()))
                .andExpect(jsonPath("$.professionalReasonRequired").value(true));
    }

    @Test
    void FR_012_cancelRequiresReason_currentAssignmentKept() throws Exception {
        long old = f.activeAssignment(resident, regular, "REGULAR", List.of(), now());
        f.propose(resident, NutritionCatalogFixture.assignmentBody(diabetes, "SOFT", List.of(), now()));
        long proposal = f.latestAssignment(resident);

        f.send(post(ASSIGNMENTS + proposal + "/cancel"), "{}").andExpect(status().isUnprocessableContent());
        f.send(post(ASSIGNMENTS + proposal + "/cancel"), "{\"reason\":\"Chờ xét nghiệm\"}")
                .andExpect(status().isOk());

        assertThat(f.assignmentStatus(proposal)).isEqualTo("CANCELLED");
        assertThat(f.assignmentStatus(old)).isEqualTo("ACTIVE");
        f.send(post(ASSIGNMENTS + proposal + "/apply"), "{\"confirmedWarnings\":[]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    @Test
    void FR_017_futureEffectiveTime_pendingThenSchedulerActivatesAndDiscontinuesOld() throws Exception {
        long old = f.activeAssignment(resident, regular, "REGULAR", List.of(), now());
        Instant from = now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        f.propose(resident, NutritionCatalogFixture.assignmentBody(diabetes, "SOFT", List.of(), from));
        long proposal = f.latestAssignment(resident);
        f.apply(proposal, "Chuẩn bị chuyển chế độ").andExpect(status().isOk());

        assertThat(f.assignmentStatus(proposal)).isEqualTo("PENDING_EFFECTIVE");
        minuteJob.runNow();
        assertThat(f.assignmentStatus(proposal)).isEqualTo("PENDING_EFFECTIVE");

        clock.setTo(from.plus(Duration.ofMinutes(3)));
        minuteJob.runNow();

        assertThat(f.assignmentStatus(proposal)).isEqualTo("ACTIVE");
        assertThat(f.assignmentStatus(old)).isEqualTo("DISCONTINUED");
        // Hiệu lực thực tế là thời điểm mong muốn khi lệnh áp dụng xảy ra trước đó, không phải lúc job chạy (FR-017)
        assertThat(jdbc.queryForObject(
                        "SELECT actual_effective_at FROM diet_assignment WHERE id = ?",
                        java.time.LocalDateTime.class,
                        proposal))
                .isEqualTo(java.time.LocalDateTime.ofInstant(from, java.time.ZoneOffset.UTC));
    }

    @Test
    void FR_017_pastEffectiveTime_actualIsCommandTime() throws Exception {
        Instant past = now().minus(3, ChronoUnit.DAYS);
        f.propose(resident, NutritionCatalogFixture.assignmentBody(regular, "REGULAR", List.of(), past));
        long proposal = f.latestAssignment(resident);
        f.apply(proposal, null).andExpect(status().isOk());

        f.asDoctor();
        mvc.perform(get("/api/v1/residents/" + resident + "/diet-assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].isDefault").value(false))
                .andExpect(jsonPath("$[0].effectiveFrom").exists())
                .andExpect(jsonPath("$[0].actualEffectiveAt").exists());
        Instant actual = jdbc.queryForObject(
                        "SELECT actual_effective_at FROM diet_assignment WHERE id = ?",
                        java.time.LocalDateTime.class,
                        proposal)
                .toInstant(java.time.ZoneOffset.UTC);
        assertThat(actual).isAfter(past.plus(1, ChronoUnit.DAYS));
    }

    @Test
    void FR_012_secondProposalWhilePending_blocked() throws Exception {
        f.propose(resident, NutritionCatalogFixture.assignmentBody(regular, "REGULAR", List.of(), now()))
                .andExpect(status().isCreated());

        f.propose(resident, NutritionCatalogFixture.assignmentBody(diabetes, "SOFT", List.of(), now()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_NOT_MET"))
                .andExpect(jsonPath("$.unmetConditions[0].code").value("PENDING_ASSIGNMENT_EXISTS"));
    }

    @Test
    void FR_011_FR_012_databaseKeepsOnePendingAndOneActivePerResident() throws Exception {
        long active = f.activeAssignment(resident, regular, "REGULAR", List.of(), now());
        f.propose(resident, NutritionCatalogFixture.assignmentBody(diabetes, "SOFT", List.of(), now()));

        assertThatThrownBy(() -> copyRow(active, "ACTIVE")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> copyRow(active, "PROPOSED")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> copyRow(active, "PENDING_EFFECTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void copyRow(long id, String status) {
        jdbc.update(
                "INSERT INTO diet_assignment (resident_id, diet_id, texture, personal_restrictions, basis,"
                        + " requested_effective_from, actual_effective_at, status, created_by, created_at)"
                        + " SELECT resident_id, diet_id, texture, personal_restrictions, basis, requested_effective_from,"
                        + " actual_effective_at, ?, created_by, created_at FROM diet_assignment WHERE id = ?",
                status,
                id);
    }

    @Test
    void FR_010_triggerBlocksContentChange() throws Exception {
        long active = f.activeAssignment(resident, regular, "REGULAR", List.of(), now());

        assertThatThrownBy(() -> jdbc.update("UPDATE diet_assignment SET texture = 'SOFT' WHERE id = ?", active))
                .hasMessageContaining("content");
        assertThatThrownBy(() -> jdbc.update("UPDATE diet_assignment SET diet_id = ? WHERE id = ?", diabetes, active))
                .hasMessageContaining("content");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE diet_assignment SET personal_restrictions = JSON_ARRAY(1) WHERE id = ?", active))
                .hasMessageContaining("content");
    }

    @Test
    void FR_010_finalAssignmentStatusCannotChange() throws Exception {
        long old = f.activeAssignment(resident, regular, "REGULAR", List.of(), now());
        f.activeAssignment(resident, f.diet(false, List.of(), safe), "PUREED", List.of(), now());
        assertThat(f.assignmentStatus(old)).isEqualTo("DISCONTINUED");

        assertThatThrownBy(() -> jdbc.update("UPDATE diet_assignment SET status = 'CANCELLED' WHERE id = ?", old))
                .hasMessageContaining("final");
    }

    @Test
    void FR_001_dietWithoutSafeDish_cannotBeUsedForNewAssignment() throws Exception {
        jdbc.update(
                "UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.safeDishId', NULL) WHERE id = ?",
                regular);

        f.propose(resident, NutritionCatalogFixture.assignmentBody(regular, "REGULAR", List.of(), now()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("SAFE_DISH_MISSING"));
    }

    @Test
    void BR_M08_03_inactiveDietOrFinalResident_blocked() throws Exception {
        long other = f.diet(false, List.of(), safe);
        f.send(post(NutritionCatalogFixture.BASE + "/diets/" + other + "/deactivate"), "{\"reason\":\"Bỏ\"}")
                .andExpect(status().isOk());
        f.propose(resident, NutritionCatalogFixture.assignmentBody(other, "REGULAR", List.of(), now()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("DIET_NOT_ACTIVE"));

        long deceased = f.resident("DECEASED");
        f.propose(deceased, NutritionCatalogFixture.assignmentBody(regular, "REGULAR", List.of(), now()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[0].code").value("RESIDENT_FINAL_STATUS"));
    }

    @Test
    void FR_002_dietStillAssigned_cannotBeDeactivated() throws Exception {
        f.activeAssignment(resident, regular, "REGULAR", List.of(), now());
        long other = f.resident("IN_RESIDENCE");
        f.propose(other, NutritionCatalogFixture.assignmentBody(diabetes, "REGULAR", List.of(), now()));

        f.send(post(NutritionCatalogFixture.BASE + "/diets/" + regular + "/deactivate"), "{\"reason\":\"Bỏ\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STILL_ASSIGNED"));
        // Đề xuất chưa áp dụng không giữ chế độ ăn (FR-002: chỉ Hiệu lực, Chờ hiệu lực)
        f.send(post(NutritionCatalogFixture.BASE + "/diets/" + diabetes + "/deactivate"), "{\"reason\":\"Bỏ\"}")
                .andExpect(status().isOk());
    }

    @Test
    void FR_063_kitchenCannotViewAssignments_nurseAndManagerCan() throws Exception {
        f.activeAssignment(resident, regular, "REGULAR", List.of(), now());
        String url = "/api/v1/residents/" + resident + "/diet-assignments";

        f.asKitchen();
        mvc.perform(get(url)).andExpect(status().isForbidden());

        TestUsers users = new TestUsers(jdbc, time);
        user.set(users.staff(Role.NURSING_STAFF).user());
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dietId").value(regular));
        user.set(users.staff(Role.NURSING_HOME_MANAGER).user());
        mvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    void FR_014_noAssignment_currentIsDefaultDietMarkedNotAssigned() throws Exception {
        long defaultDiet = f.diet(true, List.of(), safe);

        var current = dietApi.current(resident).orElseThrow();

        assertThat(current.dietId()).isEqualTo(defaultDiet);
        assertThat(current.notAssigned()).isTrue();
        assertThat(current.texture().name()).isEqualTo("REGULAR");
        assertThat(current.personalRestrictions()).isEmpty();
    }

    @Test
    void FR_010_personalRestrictionsKeptAndShownInCurrentDiet() throws Exception {
        long beef = f.allergen();
        f.activeAssignment(resident, regular, "SOFT", List.of(beef), now());

        var current = dietApi.current(resident).orElseThrow();

        assertThat(current.notAssigned()).isFalse();
        assertThat(current.personalRestrictions()).containsExactly(beef);
        assertThat(dietApi.history(resident)).hasSize(1);
    }
}
