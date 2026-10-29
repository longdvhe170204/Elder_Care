package com.eldercare.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedApiResponse;
import org.openapitools.openapidiff.core.model.ChangedContent;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.ChangedOperation;
import org.openapitools.openapidiff.core.model.ChangedParameters;
import org.openapitools.openapidiff.core.model.ChangedSchema;
import org.openapitools.openapidiff.core.model.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * R-14 (000 T038, 002 T078): OpenAPI do springdoc sinh từ mã phải khớp các hợp đồng đã dịch trong {@link #CONTRACTS};
 * endpoint chưa cài nằm ở {@code pending} của hợp đồng, phải rỗng khi xong feature. Mở rộng dần cho api-001 … api-019.
 */
@AutoConfigureMockMvc
@WithMockUser
class ApiContractIT extends MySqlIntegrationTestBase {

    /**
     * Một hợp đồng đã dịch: file, endpoint chưa cài (xóa dòng khi cài controller), phản hồi chưa cài (ví dụ 202 của cách
     * C chờ yêu cầu phê duyệt của 000). Mỗi hợp đồng so riêng với phần OpenAPI sinh ra có đường dẫn của nó, vì các hợp
     * đồng khai component cùng tên khác nội dung ({@code Problem}, {@code Reason}…).
     */
    private record Contract(Path file, Set<String> pending, Set<String> pendingResponses, boolean checkOperations) {}

    private static final List<Contract> CONTRACTS = List.of(
            new Contract(
                    Path.of("..", "specs", "000-shared-foundation", "contracts", "api-000.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 001 (đăng ký 2026-10-23 sau PR #23): đường dẫn, endpoint đã khớp; so operation tắt tới khi B sửa các chỗ
            // lệch
            // ghi ở docs/team-plan.md mục 8 (Problem theo bản chung, phản hồi 403/409/422, mã 201 của đính chính,
            // enum…)
            new Contract(
                    Path.of("..", "specs", "001-elder-profile-assessment", "contracts", "api-001.yaml"),
                    Set.of(),
                    Set.of(),
                    false),
            // 002 T025 (2026-10-20): kéo một phần T078 lên để thêm /me; danh sách giảm dần theo các user story
            new Contract(
                    Path.of("..", "specs", "002-account-permission", "contracts", "api-002.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 008 T006: kiểm ngay các controller nền; giảm PENDING khi cài từng story (phần T042 lấy sớm).
            new Contract(
                    Path.of("..", "specs", "008-staff-shift-handover", "contracts", "api-008.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 003 (A review 2026-10-26): còn chuyển giường trực tiếp (FR-028) chưa có controller
            new Contract(
                    Path.of("..", "specs", "003-room-bed-allocation", "contracts", "api-003.yaml"),
                    Set.of("POST /api/v1/bed-allocations/{id}/transfer"),
                    Set.of(),
                    true),
            // 006 (dịch 2026-10-26 sau PR #34)
            new Contract(
                    Path.of("..", "specs", "006-medication-management", "contracts", "api-006.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 009 T034 (2026-10-22, kéo phần T036 lên khi cài controller đầu tiên của US5); US3 xong 2026-10-23; lượt
            // nhắc gọi US1 xong 2026-10-26; còn "Xác nhận và tiếp nhận" chờ lệnh tiếp nhận của 007
            new Contract(
                    Path.of("..", "specs", "009-notification-service", "contracts", "api-009.yaml"),
                    Set.of("POST /api/v1/notifications/{id}/acknowledge-and-take-charge"),
                    Set.of(),
                    true),
            // 016 T028 (2026-10-24): đủ endpoint sau T009 (dashboard), khung báo cáo và danh sách chi tiết
            new Contract(
                    Path.of("..", "specs", "016-reports-dashboard", "contracts", "api-016.yaml"),
                    Set.of(),
                    Set.of(),
                    true));

    @Autowired
    MockMvc mvc;

    @Test
    void R_14_generatedApi_matchesContract() throws Exception {
        String generated =
                mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Set<String> contractPaths = new TreeSet<>();
        for (Contract c : CONTRACTS) {
            contractPaths.addAll(contract(c.file()).getPaths().keySet());
        }

        assertThat(new TreeSet<>(parse(generated).getPaths().keySet()))
                .as("đường dẫn có trong mã nhưng không có trong hợp đồng nào")
                .isSubsetOf(contractPaths);
        for (Contract c : CONTRACTS) {
            OpenAPI expected = contract(c.file());
            OpenAPI actual = parse(generated);
            actual.getPaths().keySet().retainAll(expected.getPaths().keySet());
            ChangedOpenApi diff = OpenApiCompare.fromSpecifications(expected, actual);
            String name = c.file().getFileName().toString();

            assertThat(keys(diff.getNewEndpoints()))
                    .as(name + ": endpoint có trong mã nhưng không có trong hợp đồng")
                    .isEmpty();
            Set<String> missing = keys(diff.getMissingEndpoints());
            assertThat(missing)
                    .as(name + ": endpoint chưa cài và chưa ghi PENDING")
                    .isSubsetOf(c.pending());
            assertThat(new TreeSet<>(c.pending()))
                    .as(name + ": endpoint đã cài nhưng còn ở PENDING")
                    .isEqualTo(new TreeSet<>(missing));
            if (!c.checkOperations()) {
                continue;
            }
            List<String> mismatched = new ArrayList<>();
            diff.getChangedOperations().forEach(op -> structuralChanges(op, c.pendingResponses(), mismatched));
            assertThat(mismatched).as(name + ": operation lệch hợp đồng").isEmpty();
        }
    }

    /**
     * openapi-diff chỉ báo thay đổi "không tương thích" (ví dụ bỏ trường không bắt buộc ở phản hồi vẫn là tương thích).
     * Ở đây mã phải khớp hợp đồng: không thiếu, thừa trường, kiểu, enum, trường bắt buộc, mã phản hồi, kiểu nội dung,
     * tham số; chữ mô tả được phép khác.
     */
    private static void structuralChanges(ChangedOperation op, Set<String> pendingResponses, List<String> out) {
        String at = op.getHttpMethod() + " " + op.getPathUrl();
        ChangedParameters params = op.getParameters();
        if (params != null) {
            params.getMissing().forEach(p -> out.add(at + " thiếu tham số " + p.getName()));
            params.getIncreased().forEach(p -> out.add(at + " thừa tham số " + p.getName()));
            params.getChanged().forEach(p -> schema(at + " tham số " + p.getName(), p.getSchema(), out));
        }
        if (op.getRequestBody() != null) {
            content(at + " request", op.getRequestBody().getContent(), out);
        }
        ChangedApiResponse responses = op.getApiResponses();
        if (responses != null) {
            responses.getMissing().keySet().stream()
                    .filter(code -> !pendingResponses.contains(at + " " + code))
                    .forEach(code -> out.add(at + " thiếu phản hồi " + code));
            responses.getIncreased().keySet().forEach(code -> out.add(at + " thừa phản hồi " + code));
            responses.getChanged().forEach((code, r) -> content(at + " phản hồi " + code, r.getContent(), out));
        }
    }

    private static void content(String at, ChangedContent content, List<String> out) {
        if (content == null) {
            return;
        }
        content.getMissing().keySet().forEach(type -> out.add(at + " thiếu kiểu nội dung " + type));
        content.getIncreased().keySet().forEach(type -> out.add(at + " thừa kiểu nội dung " + type));
        content.getChanged().forEach((type, media) -> schema(at + " " + type, media.getSchema(), out));
    }

    private static void schema(String at, ChangedSchema s, List<String> out) {
        if (s == null) {
            return;
        }
        s.getMissingProperties().keySet().forEach(p -> out.add(at + " thiếu trường " + p));
        s.getIncreasedProperties().keySet().forEach(p -> out.add(at + " thừa trường " + p));
        if (s.isChangedType() && !baseType(s.getOldSchema()).equals(baseType(s.getNewSchema()))) {
            out.add(at + " đổi kiểu " + baseType(s.getOldSchema()) + " → " + baseType(s.getNewSchema()));
        }
        if (s.getEnumeration() != null
                && (!s.getEnumeration().getMissing().isEmpty()
                        || !s.getEnumeration().getIncreased().isEmpty())) {
            out.add(at + " đổi enum");
        }
        if (s.getRequired() != null
                && (!s.getRequired().getMissing().isEmpty()
                        || !s.getRequired().getIncreased().isEmpty())) {
            out.add(at + " đổi trường bắt buộc");
        }
        schema(at + "[]", s.getItems(), out);
        s.getChangedProperties().forEach((name, p) -> schema(at + "." + name, p, out));
    }

    /**
     * Kiểu gốc của schema, bỏ {@code format} (int32, int64…) và {@code null} của OpenAPI 3.1: hợp đồng ghi
     * {@code integer} hay {@code [integer, 'null']}, springdoc ghi {@code integer} kèm format, cùng một kiểu.
     */
    private static Set<String> baseType(Schema<?> schema) {
        Set<String> types = new TreeSet<>();
        if (schema == null) {
            return types;
        }
        if (schema.getTypes() != null) {
            types.addAll(schema.getTypes());
        }
        if (schema.getType() != null) {
            types.add(schema.getType());
        }
        types.remove("null");
        return types;
    }

    /** Hợp đồng ghi đường dẫn tương đối với server {@code /api/v1}; mã có đủ tiền tố. */
    private static OpenAPI contract(Path file) throws IOException {
        OpenAPI api = parse(Files.readString(file, StandardCharsets.UTF_8));
        Paths prefixed = new Paths();
        api.getPaths().forEach((path, item) -> prefixed.addPathItem("/api/v1" + path, item));
        api.setPaths(prefixed);
        return api;
    }

    /** Gộp trường, trường bắt buộc của các phần {@code allOf} (đã resolve) vào chính schema, đệ quy theo trường. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void flattenAllOf(Schema schema) {
        if (schema == null) {
            return;
        }
        if (schema.getAllOf() != null) {
            List<Schema> parts = schema.getAllOf();
            schema.setAllOf(null);
            for (Schema part : parts) {
                flattenAllOf(part);
                if (part.getProperties() != null) {
                    part.getProperties().forEach((k, v) -> schema.addProperty((String) k, (Schema) v));
                }
                if (part.getRequired() != null) {
                    ((List<String>) part.getRequired()).forEach(schema::addRequiredItem);
                }
                if (schema.getType() == null && schema.getTypes() == null) {
                    schema.setType(part.getType() != null ? part.getType() : "object");
                }
            }
        }
        if (schema.getProperties() != null) {
            schema.getProperties().values().forEach(p -> flattenAllOf((Schema) p));
        }
        flattenAllOf(schema.getItems());
    }

    /**
     * Gộp {@code allOf} thành một schema ở cả hợp đồng và mã (hợp đồng 001 viết thân yêu cầu
     * {@code allOf: [PersonalInfo, {…}]}, springdoc sinh một schema phẳng) để so theo trường chứ không theo cách viết.
     */
    private static OpenAPI parse(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        OpenAPI api = new OpenAPIV3Parser().readContents(content, null, options).getOpenAPI();
        api.getPaths()
                .values()
                .forEach(item -> item.readOperations().forEach(op -> {
                    if (op.getRequestBody() != null && op.getRequestBody().getContent() != null) {
                        op.getRequestBody().getContent().values().forEach(m -> flattenAllOf(m.getSchema()));
                    }
                    op.getResponses().values().stream()
                            .filter(r -> r.getContent() != null)
                            .forEach(r -> r.getContent().values().forEach(m -> flattenAllOf(m.getSchema())));
                }));
        return api;
    }

    private static Set<String> keys(List<Endpoint> endpoints) {
        Set<String> keys = new TreeSet<>();
        endpoints.forEach(e -> keys.add(e.getMethod() + " " + e.getPathUrl()));
        return keys;
    }
}
