package com.cambae.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
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
 * R-14 (000 T038): OpenAPI do springdoc sinh từ mã phải khớp contracts/api-000.yaml. Endpoint của hợp đồng chưa cài nằm
 * ở {@link #PENDING}; danh sách này phải rỗng khi xong 000 (T103). Mở rộng dần cho api-001 … api-019.
 */
@AutoConfigureMockMvc
@WithMockUser
class ApiContractIT extends MySqlIntegrationTestBase {

    private static final Path CONTRACT = Path.of("..", "specs", "000-shared-foundation", "contracts", "api-000.yaml");

    /** Endpoint của hợp đồng 000 chưa cài; xóa dòng tương ứng khi cài controller. */
    private static final Set<String> PENDING = Set.of(
            "GET /api/v1/parameters",
            "GET /api/v1/parameters/{code}",
            "POST /api/v1/parameters/{code}/change-value",
            "GET /api/v1/parameters/{code}/history",
            "POST /api/v1/approval-requests",
            "GET /api/v1/approval-requests",
            "GET /api/v1/approval-requests/{id}",
            "POST /api/v1/approval-requests/{id}/edit-content",
            "POST /api/v1/approval-requests/{id}/submit",
            "POST /api/v1/approval-requests/{id}/approve",
            "POST /api/v1/approval-requests/{id}/reject",
            "POST /api/v1/approval-requests/{id}/cancel",
            "GET /api/v1/audit-logs",
            "GET /api/v1/audit-logs/entities/{entityType}/{entityId}");

    /** Phản hồi của hợp đồng chưa cài (ví dụ 202 của cách C chờ yêu cầu phê duyệt, US5); phải rỗng khi xong 000. */
    private static final Set<String> PENDING_RESPONSES =
            Set.of("POST /api/v1/corrections/{recordType}/{originalRecordId} 202");

    @Autowired
    MockMvc mvc;

    @Test
    void R_14_generatedApi_matchesContract() throws Exception {
        String generated =
                mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        ChangedOpenApi diff = OpenApiCompare.fromSpecifications(contract(), parse(generated));

        assertThat(keys(diff.getNewEndpoints()))
                .as("endpoint có trong mã nhưng không có trong hợp đồng")
                .isEmpty();
        Set<String> missing = keys(diff.getMissingEndpoints());
        assertThat(missing).as("endpoint hợp đồng chưa cài và chưa ghi PENDING").isSubsetOf(PENDING);
        assertThat(new TreeSet<>(PENDING))
                .as("endpoint đã cài nhưng còn ở PENDING")
                .isEqualTo(new TreeSet<>(missing));
        List<String> mismatched = new ArrayList<>();
        diff.getChangedOperations().forEach(op -> structuralChanges(op, mismatched));
        assertThat(mismatched).as("operation lệch hợp đồng").isEmpty();
    }

    /**
     * openapi-diff chỉ báo thay đổi "không tương thích" (ví dụ bỏ trường không bắt buộc ở phản hồi vẫn là tương thích).
     * Ở đây mã phải khớp hợp đồng: không thiếu, thừa trường, kiểu, enum, trường bắt buộc, mã phản hồi, kiểu nội dung,
     * tham số; chữ mô tả được phép khác.
     */
    private static void structuralChanges(ChangedOperation op, List<String> out) {
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
                    .filter(code -> !PENDING_RESPONSES.contains(at + " " + code))
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
    private static Set<String> baseType(io.swagger.v3.oas.models.media.Schema<?> schema) {
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
    private static OpenAPI contract() throws IOException {
        OpenAPI api = parse(Files.readString(CONTRACT, StandardCharsets.UTF_8));
        Paths prefixed = new Paths();
        api.getPaths().forEach((path, item) -> prefixed.addPathItem("/api/v1" + path, item));
        api.setPaths(prefixed);
        return api;
    }

    private static OpenAPI parse(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return new OpenAPIV3Parser().readContents(content, null, options).getOpenAPI();
    }

    private static Set<String> keys(List<Endpoint> endpoints) {
        Set<String> keys = new TreeSet<>();
        endpoints.forEach(e -> keys.add(e.getMethod() + " " + e.getPathUrl()));
        return keys;
    }
}
