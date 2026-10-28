package com.cambae.backend.m04;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Chỉ kiểm hợp đồng đã duyệt, không thay cho kiểm controller hoặc vòng đời kế hoạch. */
class FR_003_CarePlanContractTest {
    private OpenAPI api(String feature) throws Exception {
        return new OpenAPIV3Parser()
                .readContents(Files.readString(Path.of("../specs/" + feature)))
                .getOpenAPI();
    }

    private OpenAPI care() throws Exception {
        return api("005-daily-care-tasks/contracts/api-005.yaml");
    }

    private Schema<?> property(Schema<?> schema, String name) {
        return schema.getProperties().get(name);
    }

    @Test
    void FR_003_approvedOperationsRetainResponseCodesAndConcurrencyHeaders() throws Exception {
        var paths = care().getPaths();
        assertThat(paths)
                .containsKeys(
                        "/residents/{residentId}/care-plans",
                        "/care-plans/{id}",
                        "/care-plans/{id}/items",
                        "/care-plans/{id}/risk-agreements",
                        "/care-plans/compare");
        var create = paths.get("/residents/{residentId}/care-plans").getPost();
        assertThat(create.getOperationId()).isEqualTo("createCarePlan");
        assertThat(create.getResponses()).containsOnlyKeys("201", "409");
        assertThat(create.getResponses().get("201").getHeaders()).containsKey("ETag");
        assertThat(create.getParameters())
                .extracting(parameter -> parameter.get$ref())
                .contains("#/components/parameters/ResidentId", "#/components/parameters/IdempotencyKey");
        assertThat(paths.get("/care-plans/{id}")
                        .getGet()
                        .getResponses()
                        .get("200")
                        .getHeaders())
                .containsKey("ETag");
        for (String suffix : List.of("items", "risk-agreements")) {
            var replace = paths.get("/care-plans/{id}/" + suffix).getPut();
            assertThat(replace.getResponses()).containsOnlyKeys("200", "409", "412", "422");
            assertThat(replace.getParameters())
                    .extracting(parameter -> parameter.get$ref())
                    .contains("#/components/parameters/IfMatch", "#/components/parameters/IdempotencyKey");
        }
        for (String action : List.of("submit", "approve", "return", "cancel", "withdraw-approval")) {
            Operation operation = paths.get("/care-plans/{id}/" + action).getPost();
            assertThat(operation).isNotNull();
            assertThat(operation.getParameters())
                    .extracting(parameter -> parameter.get$ref())
                    .contains("#/components/parameters/IdempotencyKey")
                    .doesNotContain("#/components/parameters/IfMatch");
            assertThat(operation.getResponses()).containsKeys("200", "409");
        }
        assertThat(paths.get("/care-plans/compare").getGet().getParameters())
                .extracting(parameter -> parameter.getName())
                .containsExactly("from", "to");
        assertThat(paths.keySet())
                .noneMatch(path -> path.startsWith("/ke-hoach-cham-soc")
                        || path.equals("/nguoi-cao-tuoi/{nctId}/ke-hoach-cham-soc"));
    }

    @Test
    void FR_002_approvedFieldsKeepWeekdaysArrayAndExistingRequiredInputs() throws Exception {
        var schemas = care().getComponents().getSchemas();
        assertThat(schemas).containsKeys("CarePlan", "CarePlanItem", "RiskAgreement", "CarePlanComparison");
        assertThat(schemas.get("CarePlan").getProperties())
                .containsOnlyKeys(
                        "id",
                        "residentId",
                        "revision",
                        "status",
                        "effectiveFrom",
                        "effectiveTo",
                        "createdBy",
                        "approvedBy",
                        "approvedAt",
                        "selfApproved",
                        "changeReason",
                        "items",
                        "riskAgreements",
                        "noEffectivePlan",
                        "allowedCommands");
        Schema<?> item = schemas.get("CarePlanItem");
        assertThat(item.getRequired())
                .containsExactlyInAnyOrder(
                        "taskTypeId", "content", "frequencyType", "performerRole", "importance", "billable");
        assertThat(property(item, "frequencyType").getEnum())
                .extracting(Object::toString)
                .containsExactly("TIMES_PER_DAY", "EVERY_N_HOURS", "DAYS_OF_WEEK", "ONCE_PER_SHIFT");
        assertThat(property(item, "weekdays").getItems()).isNotNull();
        assertThat(schemas.get("CarePlanStatus").getEnum())
                .extracting(Object::toString)
                .containsExactly("DRAFT", "PENDING_APPROVAL", "PENDING_EFFECT", "EFFECTIVE", "EXPIRED", "CANCELLED");
        assertThat(schemas.get("CarePlanComparison").getProperties()).containsOnlyKeys("added", "removed", "changed");
    }

    @Test
    void FR_002a_riskAgreementReferencesResolveToUnmodifiedPublished001Schemas() throws Exception {
        var schemas = care().getComponents().getSchemas();
        var shared = api("001-elder-profile-assessment/contracts/api-001.yaml")
                .getComponents()
                .getSchemas();
        assertThat(schemas).containsKeys("SelfDeterminationAssessment", "OpinionSource", "RiskAgreement");
        for (String schema : List.of("SelfDeterminationAssessment", "OpinionSource")) {
            assertThat(schemas.get(schema)).isEqualTo(shared.get(schema));
        }
        Schema<?> agreement = schemas.get("RiskAgreement");
        assertThat(property(agreement, "selfDeterminationAssessment").get$ref())
                .isEqualTo("#/components/schemas/SelfDeterminationAssessment");
        assertThat(property(property(agreement, "opinions").getItems(), "source")
                        .get$ref())
                .isEqualTo("#/components/schemas/OpinionSource");
        assertThat(property(agreement, "discussedRisks").getMaxLength()).isEqualTo(2000);
        assertThat(property(property(agreement, "decisionMaker"), "reason").getMaxLength())
                .isEqualTo(1000);
    }
}
