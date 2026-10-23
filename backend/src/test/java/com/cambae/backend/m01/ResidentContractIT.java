package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T020, T032 (R-14): các endpoint hồ sơ và sức khỏe đã cài khớp {@code api-001.yaml} về đường dẫn, phương thức và tên
 * trường của {@code Profile}, {@code PersonalInfo}, {@code CreateProfileResult}, {@code HealthItem}, {@code
 * HealthItemResult}, {@code CorrectionResult}, và (T070, T080) các schema phiếu nguyện vọng. Là kiểm tạm cho phần 001 tới khi {@code ApiContractIT}
 * của 000 đăng ký api-001 (BL-23); khi đó {@code ApiContractIT} kiểm đầy đủ hơn và test này có thể bỏ.
 */
@AutoConfigureMockMvc
@WithMockUser
class ResidentContractIT extends MySqlIntegrationTestBase {

    private static final Set<String> IMPLEMENTED = Set.of(
            "GET /residents",
            "POST /residents",
            "GET /residents/{id}",
            "POST /residents/{id}/update-personal-info",
            "GET /residents/{id}/linked-profiles",
            "GET /residents/{id}/initial-health-record",
            "PUT /residents/{id}/initial-health-record",
            "POST /residents/{id}/initial-health-record/confirm",
            "POST /initial-health-records/{id}/correct",
            "GET /residents/{id}/health-items",
            "POST /residents/{id}/health-items",
            "POST /health-items/{id}/exclude",
            "GET /residents/{id}/assessments",
            "POST /residents/{id}/assessments",
            "GET /assessments/{id}",
            "PUT /assessments/{id}",
            "POST /assessments/{id}/confirm",
            "POST /assessments/{id}/discard-draft",
            "POST /assessments/{id}/correct",
            "GET /residents/{id}/care-level",
            "GET /residents/{id}/care-level-history",
            "GET /residents/{id}/status-history",
            "GET /residents/{id}/conditions/{command}",
            "POST /residents/{id}/complete-admission",
            "POST /residents/{id}/cancel-admission",
            "POST /residents/{id}/grant-temporary-absence",
            "POST /residents/{id}/record-return",
            "POST /residents/{id}/transfer-out",
            "POST /residents/{id}/end-residency",
            "POST /residents/{id}/record-death",
            "GET /residents/{id}/consents",
            "POST /residents/{id}/consents",
            "POST /consents/{id}/withdraw",
            "GET /residents/{id}/reassessment",
            "GET /reassessments/due",
            "GET /residents/{id}/end-of-life-wishes",
            "POST /residents/{id}/end-of-life-wishes",
            "GET /residents/{id}/treatment-wishes",
            "POST /residents/{id}/treatment-wishes",
            "POST /files",
            "GET /files/{id}");

    @Autowired
    MockMvc mvc;

    private static OpenAPI parse(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return new OpenAPIV3Parser().readContents(content, null, options).getOpenAPI();
    }

    private static Set<String> operations(OpenAPI api, String prefix) {
        Set<String> found = new TreeSet<>();
        api.getPaths()
                .forEach((path, item) -> item.readOperationsMap()
                        .forEach((method, op) -> found.add(method + " " + path.substring(prefix.length()))));
        return found;
    }

    private static Set<String> properties(OpenAPI api, String schema) {
        Schema<?> s = api.getComponents().getSchemas().get(schema);
        Set<String> names = new TreeSet<>();
        if (s.getProperties() != null) {
            names.addAll(s.getProperties().keySet());
        }
        return names;
    }

    @Test
    void R_14_implementedResidentEndpoints_matchContract() throws Exception {
        OpenAPI contract = parse(Files.readString(
                Path.of("..", "specs", "001-elder-profile-assessment", "contracts", "api-001.yaml"),
                StandardCharsets.UTF_8));
        OpenAPI generated = parse(
                mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        Set<String> inCode = operations(generated, "/api/v1");
        inCode.removeIf(op -> !op.contains("/residents")
                && !op.contains("/initial-health-records")
                && !op.contains("/health-items")
                && !op.contains("/assessments")
                && !op.contains("/reassessments")
                && !op.contains("/consents")
                && !op.contains("/files"));
        assertThat(inCode).containsExactlyInAnyOrderElementsOf(IMPLEMENTED);
        assertThat(operations(contract, "")).containsAll(IMPLEMENTED);

        for (String schema : new String[] {
            "Profile",
            "PersonalInfo",
            "CreateProfileResult",
            "HealthItem",
            "HealthItemResult",
            "CorrectionResult",
            "Assessment",
            "ConfirmAssessmentResult",
            "CareLevelAtDate",
            "StatusHistoryEntry",
            "StatusCommandResult",
            "Consent",
            "ReassessmentStatus",
            "EndOfLifeWish",
            "EndOfLifeWishSummary",
            "SelfDeterminationAssessment",
            "TreatmentWish",
            "TreatmentWishSummary",
            "TreatmentWishResult"
        }) {
            assertThat(properties(generated, schema)).as(schema).isEqualTo(properties(contract, schema));
        }
    }
}
