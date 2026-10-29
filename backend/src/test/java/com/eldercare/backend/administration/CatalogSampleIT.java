package com.eldercare.backend.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.sample.SampleUsers;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.testsupport.SampleDataController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

/**
 * US1 #1, #2, #8; FR-002, FR-003, FR-043, BR-M15-06 (000 T041; quickstart K20): danh mục chỉ xóa được khi chưa được
 * tham chiếu; ngừng hiệu lực rồi kích hoạt lại, chặn khi trùng tên mục đang Hiệu lực.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import({SampleDataController.class, SampleUsers.class})
class CatalogSampleIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    JsonMapper json;

    private long createItem(String name) throws Exception {
        String body = mvc.perform(post("/api/v1/test-support/sample-catalog")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body).get("id").longValue();
    }

    private long createCase(long catalogId) throws Exception {
        String body = mvc.perform(post("/api/v1/test-support/sample-cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogId\":" + catalogId + "}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body).get("id").longValue();
    }

    private int auditCount(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M15.SAMPLE_CATALOG' AND entity_id = ?",
                Integer.class,
                String.valueOf(id));
    }

    @Test
    void US1_1_deleteUnusedItem_returns204_andIsLogged() throws Exception {
        long id = createItem("Chưa dùng " + UUID.randomUUID());

        mvc.perform(delete("/api/v1/test-support/sample-catalog/" + id).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sample_catalog WHERE id = ?", Integer.class, id))
                .isZero();
        assertThat(auditCount(id)).as("tạo và xóa, mỗi lần một bản ghi nhật ký").isEqualTo(2);
    }

    @Test
    void US1_2_deleteReferencedItem_returns409_evenIfReferencedByCancelledCase() throws Exception {
        long id = createItem("Đã dùng " + UUID.randomUUID());
        long caseId = createCase(id);
        mvc.perform(post("/api/v1/test-support/sample-cases/" + caseId + "/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"tạo nhầm\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/v1/test-support/sample-catalog/" + id).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATALOG_ITEM_IN_USE"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sample_catalog WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
        assertThat(auditCount(id)).as("lần xóa bị chặn không để lại nhật ký").isEqualTo(1);
    }

    @Test
    void US1_2_deactivatedItem_notSelectableForNewCase_butStillShownInOldCase() throws Exception {
        String name = "Ngừng " + UUID.randomUUID();
        long id = createItem(name);
        long oldCase = createCase(id);

        mvc.perform(post("/api/v1/test-support/sample-catalog/" + id + "/deactivate")
                        .with(csrf()))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/test-support/sample-cases")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"catalogId\":" + id + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_NOT_MET"));
        mvc.perform(get("/api/v1/test-support/sample-cases/" + oldCase))
                .andExpect(jsonPath("$.catalogName").value(name));
        assertThat(auditCount(id)).isEqualTo(2);
    }

    @Test
    void US1_8_reactivate_whenNoActiveDuplicate_succeeds_otherwise409() throws Exception {
        String name = "Trùng " + UUID.randomUUID();
        long first = createItem(name);
        mvc.perform(post("/api/v1/test-support/sample-catalog/" + first + "/deactivate")
                        .with(csrf()))
                .andExpect(status().isOk());
        long second = createItem(name);

        mvc.perform(post("/api/v1/test-support/sample-catalog/" + first + "/reactivate")
                        .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_NOT_MET"))
                .andExpect(jsonPath("$.unmetConditions[0].code").value("BR-M15-06"));

        mvc.perform(post("/api/v1/test-support/sample-catalog/" + second + "/deactivate")
                        .with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/test-support/sample-catalog/" + first + "/reactivate")
                        .with(csrf()))
                .andExpect(status().isOk());
        assertThat(auditCount(first)).as("tạo, ngừng, kích hoạt lại").isEqualTo(3);
    }
}
