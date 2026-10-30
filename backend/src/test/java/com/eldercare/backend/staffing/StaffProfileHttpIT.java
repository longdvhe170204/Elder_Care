package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/** T006: giao dịch HTTP thật, quyền thật từ ma trận 002, bản giả các nguồn chưa triển khai. */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class StaffProfileHttpIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    JsonMapper json;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private String manager;

    @BeforeEach
    void setup() {
        var users = new TestUsers(jdbc, time);
        manager = TestUsers.bearer(
                users.session(users.staff(Role.NURSING_HOME_MANAGER).accountId()));
    }

    private long createStaff() throws Exception {
        var response = mvc.perform(
                        post("/api/v1/staff")
                                .header("Authorization", manager)
                                .header(
                                        "Idempotency-Key",
                                        java.util.UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"fullName\":\"[Nhân viên kiểm thử]\",\"position\":\"CAREGIVER\",\"phoneNumber\":\"0900000001\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return Long.parseLong(response.getHeader("Location")
                .substring(response.getHeader("Location").lastIndexOf('/') + 1));
    }

    @Test
    void FR_005_catalogCreationUsesBusinessClockForRetention() throws Exception {
        clock.setTo(java.time.Instant.parse("2000-01-01T00:00:00Z"));
        setup();
        var response = mvc.perform(post("/api/v1/credential-types")
                        .header("Authorization", manager)
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"[Loại kiểm thử đồng hồ]\",\"group\":\"TRAINING\",\"hasExpiry\":false}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        long id = Long.parseLong(response.getHeader("Location").replaceFirst(".*/", ""));
        assertThat(jdbc.queryForObject(
                                "SELECT created_at FROM credential_type WHERE id=?", java.time.LocalDateTime.class, id)
                        .toLocalDate())
                .isEqualTo(java.time.LocalDate.of(2000, 1, 1));
    }

    @Test
    void FR_001_nonCanonicalPhoneDoesNotProduceInvalidLookupKey() throws Exception {
        var response = mvc.perform(
                        post("/api/v1/staff")
                                .header("Authorization", manager)
                                .header(
                                        "Idempotency-Key",
                                        java.util.UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"fullName\":\"[Liên hệ kiểm thử]\",\"position\":\"CAREGIVER\",\"phoneNumber\":\"ext.123\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        long id = Long.parseLong(response.getHeader("Location").replaceFirst(".*/", ""));
        assertThat(jdbc.queryForObject("SELECT phone_number FROM staff_profile WHERE id=?", String.class, id))
                .isEqualTo("ext.123");
        assertThat(jdbc.queryForObject("SELECT normalized_phone FROM staff_profile WHERE id=?", String.class, id))
                .isNull();
    }

    @Test
    void FR_001_createPersistsWorkingStaffAndAuditWithoutCreatingAccount() throws Exception {
        long id = createStaff();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_profile WHERE id=?", String.class, id))
                .isEqualTo("WORKING");
        assertThat(jdbc.queryForObject("SELECT account_id FROM staff_profile WHERE id=?", Long.class, id))
                .isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.STAFF' AND entity_id=?",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(1);
        mvc.perform(get("/api/v1/staff/{id}", id).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.phoneNumber").value("0900000001"));
    }

    @Test
    void FR_001_editRequiresCurrentEtagAndNeverChangesStatus() throws Exception {
        long id = createStaff();
        String body = "{\"fullName\":\"[Nhân viên đã sửa]\",\"position\":\"CAREGIVER\",\"status\":\"RESIGNED\"}";
        mvc.perform(put("/api/v1/staff/{id}", id)
                        .header("Authorization", manager)
                        .header("If-Match", "\"0\"")
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/staff/{id}", id)
                        .header("Authorization", manager)
                        .header("If-Match", "\"0\"")
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isPreconditionFailed());
        assertThat(jdbc.queryForObject("SELECT status FROM staff_profile WHERE id=?", String.class, id))
                .isEqualTo("WORKING");
    }

    @Test
    void FR_001_onlyManagerCanCreateAndRequiredFieldsAreValidated() throws Exception {
        var users = new TestUsers(jdbc, time);
        String nurse =
                TestUsers.bearer(users.session(users.staff(Role.NURSING_STAFF).accountId()));
        mvc.perform(post("/api/v1/staff")
                        .header("Authorization", nurse)
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"A\",\"position\":\"CAREGIVER\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/staff")
                        .header("Authorization", manager)
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_052_floorManagerSeesPublicFieldsButNotUnrelatedContactDetails() throws Exception {
        long id = createStaff();
        var users = new TestUsers(jdbc, time);
        String floor =
                TestUsers.bearer(users.session(users.staff(Role.FLOOR_MANAGER).accountId()));
        mvc.perform(get("/api/v1/staff/{id}", id).header("Authorization", floor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andExpect(jsonPath("$.hiddenFields").isArray());
    }

    @Test
    void FR_006_expiryIsRequiredOnlyForTypesConfiguredWithExpiry() throws Exception {
        long id = createStaff();
        mvc.perform(post("/api/v1/staff/{id}/credentials", id)
                        .header("Authorization", manager)
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credentialTypeId\":1,\"issueDate\":\"2026-01-01\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(
                        post("/api/v1/staff/{id}/credentials", id)
                                .header("Authorization", manager)
                                .header(
                                        "Idempotency-Key",
                                        java.util.UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"credentialTypeId\":1,\"issueDate\":\"2026-01-01\",\"expiryDate\":\"2026-12-31\",\"credentialNumber\":\"TEST-001\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/staff/{id}/credentials", id).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].credentialNumber").value("TEST-001"));
        var users = new TestUsers(jdbc, time);
        String floor =
                TestUsers.bearer(users.session(users.staff(Role.FLOOR_MANAGER).accountId()));
        mvc.perform(get("/api/v1/staff/{id}/credentials", id).header("Authorization", floor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].credentialNumber").doesNotExist())
                .andExpect(jsonPath("$[0].hiddenFields").isArray());
    }
}
