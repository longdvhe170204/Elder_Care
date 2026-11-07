package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.infection.persistence.ContactListStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** BR-M05-10: chỉ lệnh của người có quyền mới lập danh sách từ sự cố nghi nhiễm đang mở. */
@Import(BR_M05_10_ContactListCommandIT.Users.class)
@Transactional
class BR_M05_10_ContactListCommandIT extends MySqlIntegrationTestBase {
    @Autowired
    WebApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    ParameterApi parameters;

    @Autowired
    ContactListStore lists;

    @Autowired
    SwitchableUser current;

    MockMvc mvc;
    long incident;
    Instant discoveredAt;

    @BeforeEach
    void prepare() {
        current.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        incident = TestUsers.nextId();
        long type = TestUsers.nextId();
        discoveredAt = time.now().truncatedTo(ChronoUnit.MICROS);
        var discovered = Timestamp.from(discoveredAt);
        jdbc.update(
                """
                INSERT INTO incident (id,resident_id,incident_type_id,current_type_id,type_code,
                  discovered_at,description,initial_level,level,recorded_by,created_at,confirmed_at)
                VALUES (?,?,?,?,'SUSPECTED_INFECTION',?,'Nghi nhiễm','MEDIUM','MEDIUM',?,?,?)
                """, incident, TestUsers.nextId(), type, type, discovered, current.accountId(), discovered, discovered);
    }

    @Test
    void BR_M05_10_createUsesDetectedTimeAndConfiguredTraceWindowOnce() throws Exception {
        mvc.perform(post("/api/v1/incidents/{id}/contact-list", incident)).andExpect(status().isCreated());
        var row = lists.findByIncident(incident).orElseThrow();
        assertThat(row.status()).isEqualTo("DRAFT");
        assertThat(row.content().traceTo()).isEqualTo(discoveredAt);
        assertThat(row.content().traceFrom())
                .isEqualTo(discoveredAt.minus(
                        parameters.read(ParameterCode.CFG_M05_07).asDuration()));
        mvc.perform(post("/api/v1/incidents/{id}/contact-list", incident)).andExpect(status().isConflict());
    }

    @Test
    void BR_M05_10_closedIncidentCannotStartContactList() throws Exception {
        jdbc.update(
                "UPDATE incident SET status='CLOSED', closed_at=? WHERE id=?", Timestamp.from(time.now()), incident);
        mvc.perform(post("/api/v1/incidents/{id}/contact-list", incident)).andExpect(status().isConflict());
        assertThat(lists.findByIncident(incident)).isEmpty();
    }

    @Test
    void BR_M05_10_managerCanViewButCannotCreateContactList() throws Exception {
        current.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        mvc.perform(post("/api/v1/incidents/{id}/contact-list", incident)).andExpect(status().isForbidden());
        assertThat(lists.findByIncident(incident)).isEmpty();
    }

    @TestConfiguration
    static class Users {
        @Bean
        SwitchableUser current() {
            return new SwitchableUser();
        }
    }
}
