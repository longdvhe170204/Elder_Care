package com.cambae.backend.m09;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class StaffingRatioHttpIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Autowired
    BusinessTime time;

    long shift;
    String manager;
    String outsider;

    @BeforeEach
    void setup() {
        var users = new TestUsers(jdbc, time);
        long used = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= used) {
            /* Tránh hồ sơ được AUTO_INCREMENT tạo từ test trước. */
        }
        manager = TestUsers.bearer(
                users.session(users.staff(Role.NURSING_HOME_MANAGER).accountId()));
        outsider = TestUsers.bearer(users.session(users.staff(Role.CAREGIVER).accountId()));
        long floor = TestUsers.nextId();
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu tỷ lệ HTTP]','07:00','15:00','FLOOR',FALSE)");
        long schedule = AssignmentFixtures.insert(
                jdbc, "INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES ('2026-10','FLOOR',?)", floor);
        shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover) VALUES (?,?,?,?,'FLOOR',?,FALSE)",
                schedule,
                template,
                LocalDateTime.of(2026, 10, 25, 0, 0),
                LocalDateTime.of(2026, 10, 25, 8, 0),
                floor);
    }

    @Test
    void FR_037_httpHistoryAndDetailReturnSameLatestCalculation_withoutWritesOnGet() throws Exception {
        jdbc.update(
                "INSERT INTO staffing_ratio_calculation(shift_id,calculated_at,event,total_weight,staff_count,ratio,threshold,meets_threshold) VALUES (?,'2026-10-25 00:00:00','DRAFT_SAVED',35,4,8.75,8,FALSE)",
                shift);
        mvc.perform(get("/api/v1/shifts/{id}/staffing-ratios", shift).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffCount").value(4))
                .andExpect(jsonPath("$[0].ratio").value(8.75))
                .andExpect(jsonPath("$[0].meetsThreshold").value(false));
        mvc.perform(get("/api/v1/shifts/{id}", shift).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffingRatio.totalWeight").value(35));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staffing_ratio_calculation WHERE shift_id=?", Integer.class, shift))
                .isEqualTo(1);
    }

    @Test
    void FR_048_noScopeDoesNotExposeHistory_unknownShiftNotFound() throws Exception {
        mvc.perform(get("/api/v1/shifts/{id}/staffing-ratios", shift).header("Authorization", outsider))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/shifts/{id}/staffing-ratios", Long.MAX_VALUE).header("Authorization", manager))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_034_zeroStaffOmitsOptionalNumericRatio_insteadOfSendingNullAgainstContract() throws Exception {
        jdbc.update(
                "INSERT INTO staffing_ratio_calculation(shift_id,calculated_at,event,total_weight,staff_count,ratio,threshold,meets_threshold) VALUES (?,'2026-10-25 00:00:00','DRAFT_SAVED',35,0,NULL,8,FALSE)",
                shift);
        mvc.perform(get("/api/v1/shifts/{id}/staffing-ratios", shift).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffCount").value(0))
                .andExpect(jsonPath("$[0].ratio").doesNotHaveJsonPath());
    }
}
