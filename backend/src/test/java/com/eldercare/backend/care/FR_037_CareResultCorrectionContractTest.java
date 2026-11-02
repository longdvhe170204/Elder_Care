package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.Test;

/** T050: chỉ kiểm bản dịch HTTP đã được chủ dự án duyệt. */
class FR_037_CareResultCorrectionContractTest {
    @Test
    void FR_037_approvedCorrectionEndpointHasExactlyTheTranslatedFields() throws Exception {
        var api = new io.swagger.v3.parser.OpenAPIV3Parser()
                .readContents(Files.readString(Path.of("../specs/005-daily-care-tasks/contracts/api-005.yaml")))
                .getOpenAPI();
        assertThat(api.getPaths())
                .containsKey("/care-results/{id}/correct")
                .doesNotContainKey("/ket-qua-ghi-nhan/{id}/dinh-chinh");
        var operation = api.getPaths().get("/care-results/{id}/correct").getPost();
        assertThat(operation.getResponses()).containsKey("201");
        assertThat(operation.getResponses().get("201").getContent()).isNullOrEmpty();
        var body = api.getComponents()
                .getRequestBodies()
                .get("CareResultCorrection")
                .getContent()
                .get("application/json")
                .getSchema();
        assertThat(body.getRequired()).containsExactlyInAnyOrder("type", "reason");
        assertThat(body.getProperties()).containsOnlyKeys("type", "correctedContent", "reason");
        var type =
                (io.swagger.v3.oas.models.media.Schema<?>) body.getProperties().get("type");
        assertThat(type.getEnum()).isEqualTo(java.util.List.of("AMEND_CONTENT", "VOID"));
    }
}
