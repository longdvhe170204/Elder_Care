package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;

/** T039: bản dịch được chủ dự án duyệt trước khi thay YAML. */
class FR_034_CareRecordingContractTest {
    @Test
    void FR_038_runtimeSnapshotResultTypeHasTheApprovedEnum() {
        var schemas = io.swagger.v3.core.converter.ModelConverters.getInstance()
                .readAll(com.eldercare.backend.care.web.response.CareTaskResponse.class);
        var field = (io.swagger.v3.oas.models.media.Schema<?>)
                schemas.get("CareTask").getProperties().get("resultType");
        assertThat(field.getEnum())
                .isEqualTo(java.util.List.of(
                        "EATING",
                        "ML",
                        "MEASURED_VALUE",
                        "MOVEMENT",
                        "SLEEP",
                        "MOOD",
                        "BEHAVIOR",
                        "ITEM_QUANTITY",
                        "PERFORMED",
                        "NONE"));
        assertThat(field.getReadOnly()).isTrue();
    }

    @Test
    void FR_038_taskExposesReadOnlySnapshotResultType() throws Exception {
        var api = new io.swagger.v3.parser.OpenAPIV3Parser()
                .readContents(Files.readString(Path.of("../specs/005-daily-care-tasks/contracts/api-005.yaml")))
                .getOpenAPI();
        var field = (io.swagger.v3.oas.models.media.Schema<?>)
                api.getComponents().getSchemas().get("CareTask").getProperties().get("resultType");
        assertThat(field).isNotNull();
        assertThat(field.getReadOnly()).isTrue();
        assertThat(field.getEnum())
                .isEqualTo(java.util.List.of(
                        "EATING",
                        "ML",
                        "MEASURED_VALUE",
                        "MOVEMENT",
                        "SLEEP",
                        "MOOD",
                        "BEHAVIOR",
                        "ITEM_QUANTITY",
                        "PERFORMED",
                        "NONE"));
    }

    @Test
    void FR_034_sixOperationsAndTheirRequiredFieldsAreTranslated() throws Exception {
        var api = new io.swagger.v3.parser.OpenAPIV3Parser()
                .readContents(Files.readString(Path.of("../specs/005-daily-care-tasks/contracts/api-005.yaml")))
                .getOpenAPI();
        assertThat(api.getPaths())
                .containsKeys(
                        "/care-tasks/{id}/claim",
                        "/checklist",
                        "/care-tasks/{id}/record",
                        "/care-tasks/{id}/not-perform",
                        "/ad-hoc-care-recordings",
                        "/residents/{residentId}/care-results");
        assertThat(api.getPaths()
                        .get("/checklist")
                        .getGet()
                        .getParameters()
                        .getFirst()
                        .getName())
                .isEqualTo("shiftId");
        var write = api.getComponents().getSchemas().get("CareResultWrite");
        assertThat(write).isNotNull();
        assertThat(write.getProperties())
                .containsOnlyKeys(
                        "performedAt",
                        "numericValue",
                        "codeValue",
                        "jsonValue",
                        "upperLimitConfirmed",
                        "note",
                        "reason");
        var checklist = api.getComponents().getSchemas().get("Checklist");
        assertThat(checklist.getProperties())
                .containsOnlyKeys(
                        "shiftId",
                        "loadedAt",
                        "tasks",
                        "riskAgreements",
                        "nonPerformanceReasonTypes",
                        "medicationDoses",
                        "openAlerts",
                        "handoverTasks");
        assertThat(api.getPaths().get("/care-tasks/{id}/record").getPost().getResponses())
                .containsOnlyKeys("200", "202", "403", "409", "422");
    }
}
