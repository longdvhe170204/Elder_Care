package com.cambae.backend.m04;

import static org.assertj.core.api.Assertions.*;

import io.swagger.v3.parser.OpenAPIV3Parser;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_038_CatalogContractTest {
    @Test
    void FR_038_approvedEnglishCatalogOperationsAndFieldsArePresent() throws Exception {
        var api = new OpenAPIV3Parser()
                .readContents(Files.readString(Path.of("../specs/005-daily-care-tasks/contracts/api-005.yaml")))
                .getOpenAPI();
        for (String path : List.of("/task-types", "/non-performance-reason-types", "/result-values")) {
            assertThat(api.getPaths()).containsKey(path);
            assertThat(api.getPaths().get(path).getGet()).isNotNull();
            assertThat(api.getPaths().get(path).getPost()).isNotNull();
            assertThat(api.getPaths().get(path + "/{id}").getPut()).isNotNull();
            assertThat(api.getPaths().get(path + "/{id}/deactivate").getPost()).isNotNull();
        }
        assertThat(api.getComponents().getSchemas().get("TaskType").getProperties())
                .containsOnlyKeys(
                        "id",
                        "name",
                        "group",
                        "professional",
                        "resultType",
                        "defaultRole",
                        "defaultImportance",
                        "windowBeforeMinutes",
                        "windowAfterMinutes",
                        "billable",
                        "serviceId",
                        "upperLimit",
                        "vitalSignType",
                        "certificates",
                        "status");
        assertThat(api.getComponents()
                        .getSchemas()
                        .get("NonPerformanceReasonType")
                        .getProperties())
                .containsOnlyKeys("id", "name", "fixed", "status");
        assertThat(api.getComponents().getSchemas().get("ResultValue").getProperties())
                .containsOnlyKeys("id", "type", "code", "name", "ruleGroup", "status");
    }
}
