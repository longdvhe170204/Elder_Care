package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * 001 T013a (FR-023h, data-model mục 6a; bổ sung 2026-10-18): không có dấu năng lực. Quét {@code information_schema} của
 * schema test và OpenAPI do springdoc sinh từ mã: bảng có {@code resident_id} (hoặc chính {@code resident}) không có cột
 * chứa {@code capacity}/{@code nang_luc}, và không schema nào của OpenAPI có trường chứa {@code nangLuc}. Chỉ phiếu
 * nguyện vọng của 001 và mục thỏa thuận của 005 mang nhận định "tự quyết được" (cột {@code can_self_determine}, không
 * chứa từ bị cấm); danh sách cho phép rỗng. Đặt tên {@code *IT} (không phải {@code *ArchTest}) vì cần MySQL; tasks ghi
 * {@code ArchTest} theo nội dung kiểm.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_023h_NoCapacityFlagIT extends MySqlIntegrationTestBase {

    /** Từ chỉ dấu năng lực chung (tên cũ và tên glossary); không gồm {@code can_self_determine}. */
    private static final List<String> FORBIDDEN = List.of("nang_luc", "nangluc", "capacity");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Test
    void FR_023h_tablesLinkedToResident_haveNoCapacityColumn() {
        List<String> linked = jdbc.queryForList("""
                SELECT DISTINCT table_name FROM information_schema.COLUMNS
                WHERE table_schema = DATABASE() AND (column_name = 'resident_id' OR table_name = 'resident')
                """, String.class);
        assertThat(linked).as("bảng gắn với người cao tuổi").contains("resident", "resident_status_history");

        List<String> offending = new ArrayList<>();
        for (String table : linked) {
            for (String column : jdbc.queryForList(
                    "SELECT column_name FROM information_schema.COLUMNS WHERE table_schema = DATABASE()"
                            + " AND table_name = ?",
                    String.class,
                    table)) {
                String lower = column.toLowerCase();
                if (FORBIDDEN.stream().anyMatch(lower::contains)) {
                    offending.add(table + "." + column);
                }
            }
        }
        assertThat(offending).as("cột dấu năng lực chung").isEmpty();
    }

    @Test
    void FR_023h_openApi_hasNoCapacityField() throws Exception {
        String raw = mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String api = raw.toLowerCase();

        assertThat(api).isNotBlank();
        for (String word : List.of("nangluc", "nang_luc")) {
            assertThat(api).as("OpenAPI chứa \"" + word + "\"").doesNotContain(word);
        }
        // Trường mang nghĩa dấu năng lực chung: tên thuộc tính của schema mô tả người cao tuổi (schema của api-001 hoặc
        // có residentId). Sức chứa của chỗ, buổi (003 DayCareSessionCapacity.capacity) không phải dấu năng lực (L207)
        Set<String> residentSchemas = parse(Files.readString(
                        Path.of("..", "specs", "001-elder-profile-assessment", "contracts", "api-001.yaml"),
                        StandardCharsets.UTF_8))
                .getComponents()
                .getSchemas()
                .keySet();
        Map<String, Schema> generated = parse(raw).getComponents().getSchemas();
        assertThat(generated).as("schema người cao tuổi").containsKey("Profile");
        List<String> offending = new ArrayList<>();
        generated.forEach((name, schema) -> {
            Set<String> fields = schema.getProperties() == null
                    ? Set.of()
                    : schema.getProperties().keySet();
            if (residentSchemas.contains(name) || fields.contains("residentId")) {
                fields.stream()
                        .filter(field -> FORBIDDEN.stream().anyMatch(field.toLowerCase()::contains))
                        .forEach(field -> offending.add(name + "." + field));
            }
        });
        assertThat(offending).as("trường dấu năng lực chung của người cao tuổi").isEmpty();
    }

    private static OpenAPI parse(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return new OpenAPIV3Parser().readContents(content, null, options).getOpenAPI();
    }
}
