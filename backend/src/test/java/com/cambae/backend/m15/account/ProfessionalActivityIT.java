package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.PracticeScopeView;
import com.cambae.backend.m15.account.api.ProfessionalActivityApi;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.cambae.backend.support.fake.m15.FakePracticeScopes;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 002 T065 (FR-036, FR-037; data-model mục 4): chỉ Quản lý viện thêm, sửa, ngừng hiệu lực mục nghiệp vụ chuyên môn,
 * kèm lý do và nhật ký; phạm vi hành nghề yêu cầu phải là mục đang hiệu lực của danh mục 008; mục mặc định không ngừng
 * hiệu lực được ({@code DEFAULT_ITEM}); sửa, ngừng hiệu lực theo {@code If-Match}. Mục thử dùng chức năng không có thao
 * tác chuyên môn trong ma trận để không ảnh hưởng test khác.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ProfessionalActivityIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakePracticeScopes practiceScopes;

    AccountFixtures fixtures;
    String manager;
    long scope;

    @BeforeEach
    void managerSignedIn() {
        fixtures = new AccountFixtures(jdbc, time);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
        scope = AccountFixtures.nextId();
        practiceScopes.item(new PracticeScopeView(scope, "Nội khoa", true));
    }

    @Test
    void FR_036_managerCreatesUpdatesDeactivates_audited() throws Exception {
        String name = "Đo điện tim " + scope;
        String created = command(post("/api/v1/professional-activities"), manager, null, input(name, scope))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.requiredPracticeScopes[0]").value(scope))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = Long.parseLong(created.replaceAll(".*\"id\":(\\d+).*", "$1"));

        command(put("/api/v1/professional-activities/" + id), manager, "\"0\"", input(name + " sửa", scope))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.name").value(name + " sửa"));
        command(put("/api/v1/professional-activities/" + id), manager, "\"0\"", input(name, scope))
                .andExpect(status().isPreconditionFailed());
        command(
                        post("/api/v1/professional-activities/" + id + "/deactivate"),
                        manager,
                        "\"1\"",
                        "{\"reason\":\"Không còn thực hiện\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type = 'M15.PROFESSIONAL_ACTIVITY' AND entity_id = ?"
                                + " ORDER BY id",
                        String.class,
                        String.valueOf(id)))
                .containsExactly(
                        "PROFESSIONAL_ACTIVITY_CREATED",
                        "PROFESSIONAL_ACTIVITY_UPDATED",
                        "PROFESSIONAL_ACTIVITY_DEACTIVATED");
    }

    @Test
    void FR_036_defaultItem_cannotBeDeactivated_listedWithDefaults() throws Exception {
        long id = jdbc.queryForObject(
                "SELECT id FROM professional_activity WHERE code = ?",
                Long.class,
                ProfessionalActivityApi.MEDICATION_ADMINISTRATION);
        long version = jdbc.queryForObject("SELECT version FROM professional_activity WHERE id = ?", Long.class, id);

        command(
                        post("/api/v1/professional-activities/" + id + "/deactivate"),
                        manager,
                        "\"" + version + "\"",
                        "{\"reason\":\"Thử\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DEFAULT_ITEM"));
        mvc.perform(get("/api/v1/professional-activities").header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem(ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING)));
    }

    @Test
    void FR_036_notManager_403_withoutReason_422_unknownPracticeScope_422() throws Exception {
        String floorManager = fixtures.session(fixtures.staffAccount("AC-02"));

        command(post("/api/v1/professional-activities"), floorManager, null, input("Thử " + scope, scope))
                .andExpect(status().isForbidden());
        command(
                        post("/api/v1/professional-activities"),
                        manager,
                        null,
                        "{\"name\":\"Thử " + scope + "\",\"function\":\"VITAL_SIGN\",\"requiredLicenseType\":\"X\","
                                + "\"requiresFacilityScope\":false}")
                .andExpect(status().isUnprocessableEntity());
        command(post("/api/v1/professional-activities"), manager, null, input("Thử " + scope, AccountFixtures.nextId()))
                .andExpect(status().isUnprocessableEntity());
    }

    private static String input(String name, long practiceScope) {
        return "{\"name\":\"" + name + "\",\"function\":\"VITAL_SIGN\",\"requiredLicenseType\":\"PRACTICE_LICENSE\","
                + "\"requiredPracticeScopes\":[" + practiceScope + "],\"requiresFacilityScope\":false,"
                + "\"reason\":\"Theo quy định mới\"}";
    }

    private ResultActions command(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String token,
            String ifMatch,
            String body)
            throws Exception {
        request.header("Authorization", bearer(token))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (ifMatch != null) {
            request.header("If-Match", ifMatch);
        }
        return mvc.perform(request);
    }
}
