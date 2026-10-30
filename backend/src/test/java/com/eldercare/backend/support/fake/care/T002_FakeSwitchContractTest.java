package com.eldercare.backend.support.fake.care;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.parser.OpenAPIV3Parser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Hợp đồng duyệt riêng 2026-10-26; không đăng ký cả phần nghiệp vụ 005 chưa dịch/chưa cài. */
class T002_FakeSwitchContractTest {
    @Test
    void T002_switchContractHasApprovedModulesBodyAndNoContentResponse() throws Exception {
        var source = Files.readString(Path.of("../specs/005-daily-care-tasks/contracts/api-005.yaml"));
        var api = new OpenAPIV3Parser().readContents(source).getOpenAPI();
        assertThat(api.getPaths()).containsKey("/testing/fakes/{module}");
        var operation = api.getPaths().get("/testing/fakes/{module}").getPut();
        assertThat(operation).isNotNull();
        assertThat(operation.getParameters().getFirst().getSchema().getEnum())
                .containsExactly("006", "007", "008", "011", "012", "014");
        var body =
                operation.getRequestBody().getContent().get("application/json").getSchema();
        assertThat(body.getRequired()).containsExactly("enabled");
        assertThat(body.getProperties()).containsOnlyKeys("enabled");
        assertThat(operation.getResponses()).containsOnlyKeys("204", "400");
        assertThat(operation.getResponses().get("204").getContent()).isNull();
    }
}
