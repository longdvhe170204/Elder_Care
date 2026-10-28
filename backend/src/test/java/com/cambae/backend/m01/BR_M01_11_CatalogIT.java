package com.cambae.backend.m01;

import static com.cambae.backend.support.TestUsers.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 001 T006, T012 (BR-M01-11, BR-M15-06; FR-018, FR-018a; data-model mục 5 "Danh mục"): danh mục dị nguyên, mức độ dị
 * ứng, nguồn thông tin, nhu cầu chăm sóc nạp sẵn theo nguồn; chỉ Quản lý viện tạo, sửa, ngừng hiệu lực, kích hoạt lại
 * (khóa quyền {@code HEALTH_CATALOGS}); hai mục Hiệu lực không trùng tên; sửa theo {@code If-Match}; ngừng hiệu lực và
 * kích hoạt lại bắt buộc lý do; mọi lệnh ghi nhật ký; mức chăm sóc cố định, chỉ đọc.
 */
@AutoConfigureMockMvc
@Transactional
class BR_M01_11_CatalogIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    String manager;
    String doctor;

    @BeforeEach
    void usersSignedIn() {
        TestUsers users = new TestUsers(jdbc, time);
        manager = users.session(users.staff(Role.NURSING_HOME_MANAGER).accountId());
        doctor = users.session(users.staff(Role.DOCTOR).accountId());
    }

    @Test
    void BR_M01_11_seeds_severityAndSourceAndCareNeedFromSource() throws Exception {
        assertThat(jdbc.queryForList("SELECT name FROM allergy_severity ORDER BY display_order", String.class))
                .startsWith("Nhẹ", "Trung bình", "Nặng");
        assertThat(jdbc.queryForList("SELECT name FROM information_source ORDER BY id", String.class))
                .startsWith("Người cao tuổi", "Người thân", "Hồ sơ y tế kèm theo", "Bác sĩ khám");
        assertThat(jdbc.queryForList("SELECT name FROM care_need ORDER BY id", String.class))
                .startsWith(
                        "Hỗ trợ vận động",
                        "Theo dõi sức khỏe thường xuyên",
                        "Phục hồi sau tai biến",
                        "Phục hồi chức năng khác");

        read("/api/v1/catalogs/allergy-severities", doctor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Nhẹ"))
                .andExpect(jsonPath("$[0].order").value(1))
                .andExpect(jsonPath("$[2].name").value("Nặng"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void FR_018a_managerCreatesUpdatesDeactivatesReactivates_audited() throws Exception {
        String name = "Penicillin " + UUID.randomUUID();
        String created = command(
                        post("/api/v1/catalogs/allergens"),
                        manager,
                        null,
                        "{\"name\":\"" + name + "\",\"group\":\"ACTIVE_INGREDIENT\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.group").value("ACTIVE_INGREDIENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = Long.parseLong(created.replaceAll(".*\"id\":(\\d+).*", "$1"));

        command(
                        put("/api/v1/catalogs/allergens/" + id),
                        manager,
                        "\"0\"",
                        "{\"name\":\"" + name + " G\",\"group\":\"ACTIVE_INGREDIENT\"}")
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.name").value(name + " G"));
        command(
                        put("/api/v1/catalogs/allergens/" + id),
                        manager,
                        "\"0\"",
                        "{\"name\":\"" + name + "\",\"group\":\"ACTIVE_INGREDIENT\"}")
                .andExpect(status().isPreconditionFailed());

        command(
                        post("/api/v1/catalogs/allergens/" + id + "/deactivate"),
                        manager,
                        null,
                        "{\"reason\":\"Trùng mục khác\"}")
                .andExpect(status().isOk());
        read("/api/v1/catalogs/allergens?status=INACTIVE", manager)
                .andExpect(jsonPath("$[*].name", hasItem(name + " G")));
        read("/api/v1/catalogs/allergens?status=ACTIVE", manager)
                .andExpect(jsonPath("$[*].name", not(hasItem(name + " G"))));
        command(post("/api/v1/catalogs/allergens/" + id + "/reactivate"), manager, null, "{\"reason\":\"Dùng lại\"}")
                .andExpect(status().isOk());

        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type = 'M01.ALLERGEN' AND entity_id = ? ORDER BY id",
                        String.class,
                        String.valueOf(id)))
                .containsExactly("CATALOG_CREATED", "CATALOG_UPDATED", "CATALOG_DEACTIVATED", "CATALOG_REACTIVATED");
    }

    @Test
    void FR_018a_listCarriesVersionSoTheClientCanSendIfMatch() throws Exception {
        String name = "Latex " + UUID.randomUUID();
        String created = command(
                        post("/api/v1/catalogs/allergens"),
                        manager,
                        null,
                        "{\"name\":\"" + name + "\",\"group\":\"OTHER\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = Long.parseLong(created.replaceAll(".*\"id\":(\\d+).*", "$1"));

        read("/api/v1/catalogs/allergens?group=OTHER", manager)
                .andExpect(jsonPath("$[?(@.id==" + id + ")].version").value(0));
        command(
                        put("/api/v1/catalogs/allergens/" + id),
                        manager,
                        "\"0\"",
                        "{\"name\":\"" + name + " 2\",\"group\":\"OTHER\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        read("/api/v1/catalogs/allergens?group=OTHER", manager)
                .andExpect(jsonPath("$[?(@.id==" + id + ")].version").value(1));
    }

    @Test
    void FR_018a_allergenListFiltersByGroup() throws Exception {
        String food = "Tôm " + UUID.randomUUID();
        String drug = "Aspirin " + UUID.randomUUID();
        command(post("/api/v1/catalogs/allergens"), manager, null, "{\"name\":\"" + food + "\",\"group\":\"FOOD\"}")
                .andExpect(status().isCreated());
        command(
                        post("/api/v1/catalogs/allergens"),
                        manager,
                        null,
                        "{\"name\":\"" + drug + "\",\"group\":\"ACTIVE_INGREDIENT\"}")
                .andExpect(status().isCreated());

        read("/api/v1/catalogs/allergens?group=FOOD", doctor)
                .andExpect(jsonPath("$[*].name", hasItem(food)))
                .andExpect(jsonPath("$[*].name", not(hasItem(drug))));
    }

    @Test
    void BR_M15_06_twoActiveItemsMayNotShareName_reactivateBlockedByActiveSameName() throws Exception {
        String name = "Cháo " + UUID.randomUUID();
        long first = createSource(name);
        command(post("/api/v1/catalogs/information-sources"), manager, null, "{\"name\":\"" + name + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_NAME"));

        command(
                        post("/api/v1/catalogs/information-sources/" + first + "/deactivate"),
                        manager,
                        null,
                        "{\"reason\":\"Nhập nhầm\"}")
                .andExpect(status().isOk());
        long second = createSource(name);
        command(
                        post("/api/v1/catalogs/information-sources/" + first + "/reactivate"),
                        manager,
                        null,
                        "{\"reason\":\"Dùng lại\"}")
                .andExpect(status().isConflict());
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void BR_M01_11_onlyManagerChangesCatalog_doctorMayRead_reasonRequiredToDeactivate() throws Exception {
        command(post("/api/v1/catalogs/care-needs"), doctor, null, "{\"name\":\"Thử " + UUID.randomUUID() + "\"}")
                .andExpect(status().isForbidden());
        read("/api/v1/catalogs/care-needs", doctor).andExpect(status().isOk());

        long id = jdbc.queryForObject("SELECT MIN(id) FROM care_need", Long.class);
        command(post("/api/v1/catalogs/care-needs/" + id + "/deactivate"), manager, null, "{}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        command(post("/api/v1/catalogs/care-needs/" + id + "/deactivate"), doctor, null, "{\"reason\":\"Thử\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_018_allergenNeedsGroup_missingItemIs404() throws Exception {
        command(
                        post("/api/v1/catalogs/allergens"),
                        manager,
                        null,
                        "{\"name\":\"Không nhóm " + UUID.randomUUID() + "\"}")
                .andExpect(status().isUnprocessableEntity());
        command(put("/api/v1/catalogs/allergens/999999999"), manager, "\"0\"", "{\"name\":\"X\",\"group\":\"FOOD\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_029_careLevels_fixedReadOnly() throws Exception {
        read("/api/v1/catalogs/care-levels", doctor)
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[*].name",
                        org.hamcrest.Matchers.contains(
                                "Chăm sóc cơ bản", "Chăm sóc thường xuyên", "Chăm sóc đặc biệt")));
        command(post("/api/v1/catalogs/care-levels"), manager, null, "{\"name\":\"Mức mới\"}")
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("READ_ONLY_CATALOG"));
    }

    private long createSource(String name) throws Exception {
        String body = command(
                        post("/api/v1/catalogs/information-sources"), manager, null, "{\"name\":\"" + name + "\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    private ResultActions read(String url, String token) throws Exception {
        return mvc.perform(get(url).header("Authorization", bearer(token)));
    }

    private ResultActions command(MockHttpServletRequestBuilder request, String token, String ifMatch, String body)
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
