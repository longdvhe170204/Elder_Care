package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.DayCareAttendanceApi;
import java.nio.file.*;
import java.time.*;
import org.junit.jupiter.api.Test;

class FR_012_AttendanceContractTest {
    @Test
    void FR_015_correctionRequestIsARealReusableComponentNotAnOperationChild() throws Exception {
        var api = new io.swagger.v3.parser.OpenAPIV3Parser()
                .readContents(Files.readString(Path.of("../specs/005-daily-care-tasks/contracts/api-005.yaml")))
                .getOpenAPI();
        var request = api.getComponents().getRequestBodies().get("DayCareAttendanceCorrection");
        assertThat(request).isNotNull();
        var schema = request.getContent().get("application/json").getSchema();
        assertThat(schema.getRequired()).containsExactlyInAnyOrder("type", "reason");
        assertThat(schema.getProperties()).containsOnlyKeys("type", "correctedContent", "reason");
        assertThat(api.getPaths().get("/day-care-attendances/check-in").getPost())
                .isNotNull();
        assertThat(api.getPaths().get("/day-care-attendances/{id}/check-out").getPost())
                .isNotNull();
    }

    @Test
    void FR_012_fourApprovedOperationsAreTranslated() throws Exception {
        String yaml = Files.readString(Path.of("../specs/005-daily-care-tasks/contracts/api-005.yaml"));
        for (String path : new String[] {
            "  /day-care-attendances:",
            "  /day-care-attendances/check-in:",
            "  /day-care-attendances/{id}/check-out:",
            "  /day-care-attendance-records/{id}/correct:"
        }) assertThat(yaml).contains(path);
        assertThat(yaml).contains("    DayCareAttendance:").doesNotContain("  /co-mat-ban-tru:");
    }

    @Test
    void FR_014_calendarSourceAndMandatoryCallbackPreserveTheExistingApi() throws Exception {
        assertThatCode(() -> Class.forName("com.eldercare.backend.care.api.spi.DayCareCalendarPort"))
                .doesNotThrowAnyException();
        assertThat(DayCareAttendanceApi.class.getMethod("refreshCalendar", long.class, LocalDate.class))
                .isNotNull();
        assertThat(DayCareAttendanceApi.class.getMethod("returnTimeOn", long.class, LocalDate.class))
                .isNotNull();
    }
}
