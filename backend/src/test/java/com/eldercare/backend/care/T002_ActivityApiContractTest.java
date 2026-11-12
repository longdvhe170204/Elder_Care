package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T002 (contracts mục 1.2, 1.3; chủ dự án duyệt 2026-11-05 đặt sự kiện ở care.api.events, tính phí dùng
 * billing.api.events.CostSourceBilled có sẵn): giao diện, sự kiện công khai và bản giả cho module làm plan sau.
 */
class T002_ActivityApiContractTest {
    static final String API = "com.eldercare.backend.care.api.";

    @Test
    void T002_newInterfacesArePublicWithTheirOperations() throws Exception {
        Map<String, Integer> contracts = Map.of("TripDutyCommandApi", 1, "ActivityStatisticsApi", 4);
        for (var entry : contracts.entrySet()) {
            Class<?> api = Class.forName(API + entry.getKey());
            assertThat(api.isInterface()).as(entry.getKey()).isTrue();
            assertThat(Modifier.isPublic(api.getModifiers())).isTrue();
            assertThat(api.getDeclaredMethods()).as(entry.getKey()).hasSize(entry.getValue());
        }
        var duty = Class.forName(API + "TripDutyCommandApi");
        assertThat(duty.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
        assertThat(Arrays.stream(duty.getDeclaredMethods()).map(m -> m.getName()))
                .containsExactly("removeDuty");
        assertThat(Arrays.stream(Class.forName(API + "ActivityStatisticsApi").getDeclaredMethods())
                        .map(m -> m.getName()))
                .containsExactlyInAnyOrder("sessions", "trips", "qualityChecks", "outOfCheckScope");
    }

    @Test
    void T002_eventsKeepTheContractFields() throws Exception {
        Map<String, List<String>> events = Map.of(
                "TripChanged",
                List.of(
                        "tripId",
                        "type",
                        "facilityVehicle",
                        "plannedDepartureAt",
                        "plannedReturnAt",
                        "actualAt",
                        "eventKey"),
                "AlertProgressRequested",
                List.of("sourceType", "sourceId", "progressCode", "content", "eventKey"),
                "ActivitySessionCancelled",
                List.of("sessionId"),
                "ActivitySessionChanged",
                List.of("sessionId"),
                "ActivitySessionEnded",
                List.of("sessionId"));
        for (var entry : events.entrySet()) {
            Class<?> type = Class.forName(API + "events." + entry.getKey());
            assertThat(type.isRecord()).as(entry.getKey()).isTrue();
            assertThat(Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName))
                    .as(entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
        }
    }
}
