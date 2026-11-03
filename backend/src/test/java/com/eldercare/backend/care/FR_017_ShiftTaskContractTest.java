package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.spi.*;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.nio.file.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FR_017_ShiftTaskContractTest {
    @Test
    void FR_017_approvedOperationsAndRowErrorsUseEnglishNames() throws Exception {
        var api = new OpenAPIV3Parser()
                .readContents(Files.readString(Path.of("../specs/005-daily-care-tasks/contracts/api-005.yaml")))
                .getOpenAPI();
        assertThat(api.getPaths())
                .containsKeys(
                        "/shifts/{shiftId}/source-data",
                        "/shifts/{shiftId}/care-tasks",
                        "/shifts/{shiftId}/care-tasks/plan",
                        "/care-tasks/ad-hoc",
                        "/care-tasks/{id}/rework",
                        "/care-tasks/{id}",
                        "/care-tasks/{id}/cancel");
        var write = api.getComponents().getSchemas().get("CareTaskWrite");
        assertThat(write).isNotNull();
        assertThat(write.getRequired()).containsExactlyInAnyOrder("taskTypeId", "basis", "plannedAt", "importance");
        assertThat(write.getProperties())
                .containsOnlyKeys(
                        "residentId",
                        "location",
                        "taskTypeId",
                        "basis",
                        "content",
                        "plannedAt",
                        "windowBeforeMinutes",
                        "windowAfterMinutes",
                        "importance",
                        "performerId",
                        "shared");
        assertThat(api.getPaths().keySet()).noneMatch(p -> p.contains("suggest") || p.contains("copy-tasks"));
        assertThat(api.getComponents().getSchemas()).containsKey("CareTaskRowProblem");
    }

    @Test
    void FR_017_approvedPortsCanReadActualSourceIdsAndShiftMemberRoles() {
        assertThatCode(() -> TaskSourcePort.class.getMethod("sources", long.class, Instant.class, Instant.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> ShiftStaffPort.class.getMethod("members", long.class))
                .doesNotThrowAnyException();
    }
}
