package com.eldercare.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.eldercare.backend.support.MySqlIntegrationTestBase;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
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
                    Set.of(),
                    Set.of(),
                    true),
            // 008 T006: kiểm ngay các controller nền; giảm PENDING khi cài từng story (phần T042 lấy sớm).
            new Contract(
                    Path.of("..", "specs", "008-staff-shift-handover", "contracts", "api-008.yaml"),
                    Set.of(),
                    // MyShift.scopeEndsAt (chủ dự án duyệt 2026-11-08): E cài thì xóa
                    Set.of("GET /api/v1/me/shifts 200"),
                    true),
            // 003 (chủ dự án duyệt 2026-11-06, A review): chuyển thường đã cài; phản hồi nhập bù chờ T034.
            new Contract(
                    Path.of("..", "specs", "003-room-bed-allocation", "contracts", "api-003.yaml"),
                    Set.of(),
                    Set.of("POST /api/v1/bed-allocations/{id}/transfer 202"),
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
                    Set.of(),
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
            // 011 (2026-11-08, D): đủ mọi operation (bữa ăn cài sau khi A thêm NUTRITION_CATALOG, Q-401)
            new Contract(
                    Path.of("..", "specs", "011-nutrition-management", "contracts", "api-011.yaml"),
                    Set.of(),
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
                            "POST /api/v1/balance-transactions",
                            "GET /api/v1/balance-transactions/pending-approval",
                            "POST /api/v1/residents/{residentId}/deposit-offsets",
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
                            "GET /api/v1/family-portal/residents/{residentId}/documents",
                            "GET /api/v1/family-portal/documents/{type}/{id}/file",
                            "POST /api/v1/family-portal/residents/{residentId}/balance-statement"),
                    Set.of(),
                    true),
            // 014 (2026-11-05): hợp đồng đã dịch (glossary mục 20); xóa dòng khi cài controller của từng story
            new Contract(
                    Path.of("..", "specs", "014-activity-care-quality", "contracts", "api-014.yaml"),
                    Set.of(
                            "GET /api/v1/activity-types",
                            "POST /api/v1/activity-types",
                            "POST /api/v1/late-return-consents",
                            "POST /api/v1/late-return-consents/{id}/withdraw",
                            "GET /api/v1/residents/{residentId}/preferences",
                            "GET /api/v1/residents/{residentId}/wellbeing-profile",
                            "GET /api/v1/family-portal/residents/{residentId}/activities",
                            "POST /api/v1/family-portal/late-return-consents",
                            "POST /api/v1/family-portal/late-return-consents/{id}/withdraw"),
                    Set.of(),
                    true),
            // 019 (2026-11-06): hợp đồng đã dịch (glossary mục 25); xóa dòng khi cài controller của từng story
            new Contract(
                    Path.of("..", "specs", "019-inventory-assets", "contracts", "api-019.yaml"),
                    Set.of("GET /api/v1/assets/{id}"),
                    Set.of(),
                    true),
            // 015 (D, 2026-11-09, T028): hợp đồng đã dịch (glossary mục 21); đủ endpoint đổi ca và nghỉ đột xuất
            new Contract(
                    Path.of("..", "specs", "015-shift-rotation-swap", "contracts", "api-015.yaml"),
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
        List<String> mismatched = new ArrayList<>();
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
            List<String> found = new ArrayList<>();
            diff.getChangedOperations().forEach(op -> structuralChanges(op, c.pendingResponses(), found));
            bodies(expected, actual, c.pendingResponses(), found);
            found.forEach(m -> mismatched.add(name + ": " + m));
        }
        Set<String> known = knownMismatches();
        assertThat(mismatched.stream().filter(m -> !known.contains(m)).toList())
                .as("operation lệch hợp đồng")
                .isEmpty();
        assertThat(known.stream().filter(k -> !mismatched.contains(k)).toList())
                .as("lệch đã hết nhưng còn ở contract/known-mismatches.txt: xóa dòng")
                .isEmpty();
    }

    /** Lệch có sẵn khi bắt đầu so sâu (2026-11-08), mỗi chủ hợp đồng gỡ dần; dòng {@code #} là chú thích. */
    private static Set<String> knownMismatches() throws IOException {
        try (var in = ApiContractIT.class.getResourceAsStream("/contract/known-mismatches.txt")) {
            Set<String> known = new TreeSet<>();
            new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .lines()
                    .map(String::strip)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .forEach(known::add);
            return known;
        }
    }

    /**
     * So schema thân yêu cầu, phản hồi theo trường, phần tử mảng, kiểu, enum, trường bắt buộc (đệ quy). Không dùng
     * openapi-diff ở tầng này: với OpenAPI 3.1 nó bỏ qua {@code items} (danh sách trả về, trường mảng) và schema đã gộp
     * {@code allOf} (phát hiện 2026-11-08: trường thừa trong {@code ProcurementRequest.lines[]} vẫn xanh). Trường
     * {@code readOnly} không xét ở thân yêu cầu, {@code writeOnly} không xét ở phản hồi.
     */
    private static void bodies(OpenAPI expected, OpenAPI actual, Set<String> pendingResponses, List<String> out) {
        expected.getPaths().forEach((path, item) -> {
            PathItem other = actual.getPaths().get(path);
            if (other == null) {
                return;
            }
            item.readOperationsMap().forEach((method, op) -> {
                Operation code = other.readOperationsMap().get(method);
                if (code == null) {
                    return;
                }
                String at = method + " " + path;
                if (op.getRequestBody() != null && code.getRequestBody() != null) {
                    media(
                            at + " request",
                            op.getRequestBody().getContent(),
                            code.getRequestBody().getContent(),
                            true,
                            out);
                }
                if (op.getResponses() == null || code.getResponses() == null) {
                    return;
                }
                op.getResponses().forEach((status, response) -> {
                    var codeResponse = code.getResponses().get(status);
                    if (codeResponse != null && !pendingResponses.contains(at + " " + status)) {
                        media(at + " phản hồi " + status, response.getContent(), codeResponse.getContent(), false, out);
                    }
                });
            });
        });
    }

    private static void media(String at, Content expected, Content actual, boolean request, List<String> out) {
        if (expected == null || actual == null) {
            return;
        }
        expected.forEach((type, m) -> {
            var other = actual.get(type);
            if (other != null) {
                compare(at + " " + type, m.getSchema(), other.getSchema(), request, 0, out);
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void compare(
            String at, Schema expected, Schema actual, boolean request, int depth, List<String> out) {
        if (expected == null || actual == null || depth > 20) {
            return;
        }
        // springdoc không ghi type: object cho object; chỉ so khi cả hai bên ghi kiểu, enum
        Set<String> te = baseType(expected);
        Set<String> ta = baseType(actual);
        if (!te.isEmpty() && !ta.isEmpty() && !te.equals(ta)) {
            out.add(at + " đổi kiểu " + te + " → " + ta);
        }
        Set<String> ve = values(expected.getEnum());
        Set<String> va = values(actual.getEnum());
        if (!ve.isEmpty() && !va.isEmpty() && !ve.equals(va)) {
            out.add(at + " đổi enum");
        }
        java.util.Map<String, Schema> e = fields(expected, request);
        java.util.Map<String, Schema> a = fields(actual, request);
        if (e.isEmpty()) {
            // hợp đồng để object tự do (không liệt kê trường): không so trường
            compare(at + "[]", expected.getItems(), actual.getItems(), request, depth + 1, out);
            return;
        }
        if (!required(expected, e).equals(required(actual, a))) {
            out.add(at + " đổi trường bắt buộc");
        }
        e.keySet().stream().filter(k -> !a.containsKey(k)).forEach(k -> out.add(at + " thiếu trường " + k));
        a.keySet().stream().filter(k -> !e.containsKey(k)).forEach(k -> out.add(at + " thừa trường " + k));
        e.forEach((k, p) -> {
            if (a.containsKey(k)) {
                compare(at + "." + k, p, a.get(k), request, depth + 1, out);
            }
        });
        compare(at + "[]", expected.getItems(), actual.getItems(), request, depth + 1, out);
    }

    /** Trường có ở thân này: bỏ {@code readOnly} ở yêu cầu, {@code writeOnly} ở phản hồi. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static java.util.Map<String, Schema> fields(Schema schema, boolean request) {
        java.util.Map<String, Schema> result = new java.util.TreeMap<>();
        if (schema.getProperties() != null) {
            ((java.util.Map<String, Schema>) schema.getProperties()).forEach((k, p) -> {
                boolean hidden = request ? Boolean.TRUE.equals(p.getReadOnly()) : Boolean.TRUE.equals(p.getWriteOnly());
                if (!hidden) {
                    result.put(k, p);
                }
            });
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static Set<String> required(Schema schema, java.util.Map<String, Schema> fields) {
        Set<String> result = new TreeSet<>();
        if (schema.getRequired() != null) {
            for (Object name : schema.getRequired()) {
                if (fields.containsKey(name)) {
                    result.add((String) name);
                }
            }
        }
        return result;
    }

    private static Set<String> values(List<?> values) {
        Set<String> result = new TreeSet<>();
        if (values != null) {
            values.stream().filter(java.util.Objects::nonNull).forEach(v -> result.add(v.toString()));
        }
        return result;
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
            responses.getChanged().entrySet().stream()
                    // phản hồi đã đổi ở hợp đồng mà mã chưa theo kịp cũng chờ như phản hồi chưa cài
                    .filter(e -> !pendingResponses.contains(at + " " + e.getKey()))
                    .forEach(e ->
                            content(at + " phản hồi " + e.getKey(), e.getValue().getContent(), out));
        }
    }

    private static void content(String at, ChangedContent content, List<String> out) {
        if (content == null) {
            return;
        }
        content.getMissing().keySet().forEach(type -> out.add(at + " thiếu kiểu nội dung " + type));
        content.getIncreased().keySet().forEach(type -> out.add(at + " thừa kiểu nội dung " + type));
        // schema trong nội dung so ở bodies()
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
