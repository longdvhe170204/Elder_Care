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
            // 001 (đăng ký 2026-10-23 sau PR #23; so operation bật 2026-10-24 khi B hết các chỗ lệch, 001 T071)
            new Contract(
                    Path.of("..", "specs", "001-elder-profile-assessment", "contracts", "api-001.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 002 T025 (2026-10-20): kéo một phần T078 lên để thêm /me; danh sách giảm dần theo các user story
            new Contract(
                    Path.of("..", "specs", "002-account-permission", "contracts", "api-002.yaml"),
                    // Q-399 (2026-11-03): tự đặt lại mật khẩu, cài khi chốt nhà cung cấp tin nhắn (Q-06, 002 T105)
                    Set.of(
                            "GET /api/v1/auth/password-reset/availability",
                            "POST /api/v1/auth/password-reset/request",
                            "POST /api/v1/auth/password-reset/confirm"),
                    Set.of(),
                    true),
            // 008 T006: kiểm ngay các controller nền; giảm PENDING khi cài từng story (phần T042 lấy sớm).
            new Contract(
                    Path.of("..", "specs", "008-staff-shift-handover", "contracts", "api-008.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 003 (chủ dự án duyệt 2026-11-06, A review): chuyển thường đã cài; phản hồi nhập bù 202 xong 2026-11-08
            // (T034).
            new Contract(
                    Path.of("..", "specs", "003-room-bed-allocation", "contracts", "api-003.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 004 (dịch lại 2026-11-02 theo Q-394, Q-395, R-15; A review trong PR); sửa yêu cầu Nháp xong 2026-11-04
            // (T043, T047)
            new Contract(
                    Path.of("..", "specs", "004-admission-residency", "contracts", "api-004.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 005 T060 (2026-11-03); T056, T058 (2026-11-05) dịch và cài phân lại, thời khóa biểu; còn /testing/fakes
            new Contract(
                    Path.of("..", "specs", "005-daily-care-tasks", "contracts", "api-005.yaml"),
                    Set.of("PUT /api/v1/testing/fakes/{module}"),
                    Set.of(),
                    true),
            // 006 (dịch 2026-10-26 sau PR #34)
            new Contract(
                    Path.of("..", "specs", "006-medication-management", "contracts", "api-006.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 007 T062 (dịch đủ 2026-11-03, bản dịch contracts/api-translation.md): danh mục và đo chỉ số đã cài
            new Contract(
                    Path.of("..", "specs", "007-health-alert-incident", "contracts", "api-007.yaml"),
                    Set.of(
                            "GET /api/v1/incidents/{id}/family-contacts-to-notify",
                            "GET /api/v1/residents/{residentId}/examinations",
                            "POST /api/v1/residents/{residentId}/examinations"),
                    Set.of(),
                    true),
            // 009 T034 (2026-10-22, kéo phần T036 lên khi cài controller đầu tiên của US5); US3 xong 2026-10-23; lượt
            // nhắc gọi US1 xong 2026-10-26; "Xác nhận và tiếp nhận" xong 2026-11-06 (T021)
            new Contract(
                    Path.of("..", "specs", "009-notification-service", "contracts", "api-009.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 016 T028 (2026-10-24): đủ endpoint sau T009 (dashboard), khung báo cáo và danh sách chi tiết
            new Contract(
                    Path.of("..", "specs", "016-reports-dashboard", "contracts", "api-016.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 013 (B, 2026-11-05): đủ endpoint sau Phase 8; chủ dự án duyệt api-013 2026-11-05 (013-1), so chi tiết
            // operation bật trong PR 013 (T029). Phản hồi lỗi khai bằng ProblemBody chung sau khi đổi Problem của
            // api-013 theo api-004 (2026-11-06, phương án (a), A review xong 2026-11-07; plan 013 điểm 21).
            new Contract(
                    Path.of("..", "specs", "013-personal-belongings", "contracts", "api-013.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 012 (2026-11-03): hợp đồng đã dịch; xóa dòng khi cài controller của từng story (US1, US3 xong 2026-11-04;
            // US2, US4, US5, US6, US9 2026-11-05; US8 2026-11-05)
            new Contract(
                    Path.of("..", "specs", "012-family-portal", "contracts", "api-012.yaml"), Set.of(), Set.of(), true),
            // 011 (2026-11-06, D): hợp đồng đã dịch; xóa dòng khi cài controller của từng story. Bữa ăn, loại đồ ăn bị
            // cấm
            // chờ chức năng quyền của Quản lý viện (A, team-plan mục 8)
            new Contract(
                    Path.of("..", "specs", "011-nutrition-management", "contracts", "api-011.yaml"),
                    Set.of(
                            "GET /api/v1/nutrition/meals",
                            "POST /api/v1/nutrition/meals",
                            "PUT /api/v1/nutrition/meals/{id}"),
                    Set.of(),
                    true),
            // 018 (R-15, 2026-11-04; A xem xong 2026-11-07, BL-91): đủ bốn operation, năm lệnh {command}; lệnh ngoài
            // enum trả 422
            // INVALID_COMMAND (ngoài hợp đồng, BL-95)
            new Contract(
                    Path.of("..", "specs", "018-residence-registration", "contracts", "api-018.yaml"),
                    Set.of(),
                    Set.of(),
                    true),
            // 017 (2026-11-08, A): hợp đồng đã dịch (glossary mục 23); xóa dòng khi cài controller của từng story (A
            // Phase 1–6,
            // B Phase 7–10)
            new Contract(
                    Path.of("..", "specs", "017-resident-balance-ledger", "contracts", "api-017.yaml"),
                    Set.of(
                            "GET /api/v1/residents/{residentId}/balance",
                            "GET /api/v1/residents/{residentId}/balance-transactions",
                            "GET /api/v1/banks",
                            "GET /api/v1/balances/by-payment-code",
                            "POST /api/v1/residents/{residentId}/cash-payments",
                            "POST /api/v1/residents/{residentId}/bank-transfer-payments",
                            "POST /api/v1/balance-transactions",
                            "GET /api/v1/balance-transactions/pending-approval",
                            "POST /api/v1/residents/{residentId}/deposit-offsets",
                            "GET /api/v1/balance-statuses",
                            "POST /api/v1/residents/{residentId}/balance/stop-reminders",
                            "GET /api/v1/residents/{residentId}/settlement",
                            "POST /api/v1/residents/{residentId}/settlement/confirm",
                            "POST /api/v1/settlement-statements/{id}/signed-copy",
                            "GET /api/v1/settlement-statements/{id}/file",
                            "GET /api/v1/payment-notices/pending-issue",
                            "POST /api/v1/costs/statements/{statementId}/payment-notice",
                            "GET /api/v1/residents/{residentId}/payment-notices",
                            "GET /api/v1/payment-notices/{id}/file",
                            "POST /api/v1/balance-exports/statement",
                            "POST /api/v1/balance-exports/income-expense-report",
                            "GET /api/v1/balance-exports/{id}/file",
                            "GET /api/v1/family-portal/residents/{residentId}/balance",
                            "GET /api/v1/family-portal/residents/{residentId}/documents",
                            "GET /api/v1/family-portal/documents/{type}/{id}/file",
                            "POST /api/v1/family-portal/residents/{residentId}/balance-statement"),
                    Set.of(),
                    true),
            // 014 (2026-11-05): hợp đồng đã dịch (glossary mục 20); xóa dòng khi cài controller của từng story
            new Contract(
                    Path.of("..", "specs", "014-activity-care-quality", "contracts", "api-014.yaml"),
                    Set.of("GET /api/v1/family-portal/residents/{residentId}/activities"),
                    Set.of(),
                    true),
            // 019 (2026-11-06): hợp đồng đã dịch (glossary mục 25); xóa dòng khi cài controller của từng story
            new Contract(
                    Path.of("..", "specs", "019-inventory-assets", "contracts", "api-019.yaml"),
                    Set.of("GET /api/v1/assets/{id}"),
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
