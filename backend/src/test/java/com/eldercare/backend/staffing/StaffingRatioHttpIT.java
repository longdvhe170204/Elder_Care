package com.eldercare.backend.staffing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
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
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu tỷ lệ HTTP] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,FALSE AS requires_handover) src");
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
                "INSERT INTO record_event(entity_type,entity_id,module,kind,by_system,occurred_at,confirmed_at,content) SELECT 'M09.SHIFT',src.shift_id,'M09','M09.STAFFING_RATIO_CALCULATION',FALSE,src.calculated_at,src.calculated_at,JSON_OBJECT('calculatedAt',DATE_FORMAT(src.calculated_at,'%Y-%m-%dT%H:%i:%s.%fZ'),'event',src.event,'totalWeight',src.total_weight,'staffCount',src.staff_count,'ratio',src.ratio,'threshold',src.threshold,'meetsThreshold',IF(src.meets_threshold,CAST('true' AS JSON),CAST('false' AS JSON))) FROM (SELECT ? AS shift_id,'2026-10-25 00:00:00' AS calculated_at,'DRAFT_SAVED' AS event,35 AS total_weight,4 AS staff_count,8.75 AS ratio,8 AS threshold,FALSE AS meets_threshold) src",
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
                        "SELECT COUNT(*) FROM (SELECT COALESCE(CAST(NULLIF(JSON_UNQUOTE(e.content->'$.legacy.id'),'null') AS UNSIGNED),e.id) AS id,e.entity_id AS shift_id,e.occurred_at AS calculated_at,NULLIF(JSON_UNQUOTE(e.content->'$.event'),'null') AS event,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.totalWeight'),'null') AS DECIMAL(8,2)) AS total_weight,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.staffCount'),'null') AS SIGNED) AS staff_count,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.ratio'),'null') AS DECIMAL(8,2)) AS ratio,CAST(NULLIF(JSON_UNQUOTE(e.content->'$.threshold'),'null') AS DECIMAL(8,2)) AS threshold,(e.content->'$.meetsThreshold'=CAST('true' AS JSON)) AS meets_threshold FROM record_event e WHERE e.kind='M09.STAFFING_RATIO_CALCULATION') staffing_ratio_calculation WHERE shift_id=?",
                        Integer.class,
                        shift))
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
                "INSERT INTO record_event(entity_type,entity_id,module,kind,by_system,occurred_at,confirmed_at,content) SELECT 'M09.SHIFT',src.shift_id,'M09','M09.STAFFING_RATIO_CALCULATION',FALSE,src.calculated_at,src.calculated_at,JSON_OBJECT('calculatedAt',DATE_FORMAT(src.calculated_at,'%Y-%m-%dT%H:%i:%s.%fZ'),'event',src.event,'totalWeight',src.total_weight,'staffCount',src.staff_count,'ratio',src.ratio,'threshold',src.threshold,'meetsThreshold',IF(src.meets_threshold,CAST('true' AS JSON),CAST('false' AS JSON))) FROM (SELECT ? AS shift_id,'2026-10-25 00:00:00' AS calculated_at,'DRAFT_SAVED' AS event,35 AS total_weight,0 AS staff_count,NULL AS ratio,8 AS threshold,FALSE AS meets_threshold) src",
                shift);
        mvc.perform(get("/api/v1/shifts/{id}/staffing-ratios", shift).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffCount").value(0))
                .andExpect(jsonPath("$[0].ratio").doesNotHaveJsonPath());
    }
}
