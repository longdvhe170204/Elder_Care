package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/** Lệnh khoanh vùng qua HTTP dùng quyền, nhật ký, sự cố và nguồn phân bổ thật. */
@Import(ContainmentCommandIT.Users.class)
@Transactional
class ContainmentCommandIT extends MySqlIntegrationTestBase {
    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    AllocationInfectionStatus infections;

    MockMvc mvc;
    long floor;
    long incident;

    @BeforeEach
    void prepare() {
        current.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        long area = TestUsers.nextId();
        floor = TestUsers.nextId();
        incident = TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES (?,?,?)", area, "A" + area, "Khu kiểm thử");
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES (?,?,?,?)", floor, area, "F" + floor, "Tầng kiểm thử");
        var at = Timestamp.from(time.now());
        long type = TestUsers.nextId();
        jdbc.update("""
                INSERT INTO incident (id,resident_id,incident_type_id,current_type_id,type_code,
                  discovered_at,description,initial_level,level,recorded_by,created_at,confirmed_at)
                VALUES (?,?,?,?,'SUSPECTED_INFECTION',?,'Nghi nhiễm','MEDIUM','MEDIUM',?,?,?)
                """, incident, TestUsers.nextId(), type, type, at, current.accountId(), at, at);
    }

    @Test
    void BR_M05_11_createAndLiftUpdateAllocationAndAuditInTheSameTransaction() throws Exception {
        mvc.perform(post("/api/v1/isolations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated());
        var result = mvc.perform(get("/api/v1/isolations"))
                .andExpect(status().isOk())
                .andReturn();
        var rows = JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
        long id = -1;
        for (var row : rows) {
            if (row.get("incidentIds").get(0).asLong() == incident)
                id = row.get("id").asLong();
        }
        assertThat(id).isPositive();
        assertThat(infections.cordonedAt(time.now()).floorIds()).contains(floor);
        assertThat(audits(id)).isEqualTo(1);

        mvc.perform(post("/api/v1/isolations/{id}/attach-incident", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incidentId\":" + incident + "}"))
                .andExpect(status().isOk());
        assertThat(audits(id)).isEqualTo(1);

        mvc.perform(post("/api/v1/isolations/{id}/lift", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Đủ điều kiện gỡ\"}"))
                .andExpect(status().isOk());
        assertThat(infections.cordonedAt(time.now()).floorIds()).doesNotContain(floor);
        assertThat(audits(id)).isEqualTo(2);
        mvc.perform(post("/api/v1/isolations/{id}/lift", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Gỡ lại\"}"))
                .andExpect(status().isConflict());
        assertThat(audits(id)).isEqualTo(2);
    }

    @Test
    void FR_063_closedIncidentAndNursingStaffCannotCreateContainment() throws Exception {
        jdbc.update(
                "UPDATE incident SET status='CLOSED', closed_at=? WHERE id=?", Timestamp.from(time.now()), incident);
        mvc.perform(post("/api/v1/isolations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isConflict());
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
        mvc.perform(post("/api/v1/isolations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_063_missingReasonOrMixedScopeDoesNotCreateContainment() throws Exception {
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM simple_record WHERE kind='M05.CONTAINMENT'", Long.class);
        mvc.perform(post("/api/v1/isolations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":{\"floorIds\":[" + floor + "]},\"incidentIds\":[" + incident + "]}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/isolations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scope\":{\"floorIds\":[" + floor + "],\"areaId\":1},\"incidentIds\":[" + incident
                                + "],\"reason\":\"Khoanh vùng\"}"))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM simple_record WHERE kind='M05.CONTAINMENT'", Long.class))
                .isEqualTo(count);
    }

    private String body() {
        return "{\"scope\":{\"floorIds\":[" + floor + "]},\"incidentIds\":[" + incident
                + "],\"reason\":\"Khoanh vùng\"}";
    }

    private long audits(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type='M05.CONTAINMENT' AND entity_id=?",
                Long.class,
                Long.toString(id));
    }

    @TestConfiguration
    static class Users {
        @Bean
        SwitchableUser current() {
            return new SwitchableUser();
        }
    }
}
