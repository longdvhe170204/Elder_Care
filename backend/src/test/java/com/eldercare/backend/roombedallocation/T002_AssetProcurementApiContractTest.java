package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.api.dto.ProcurementRequestView;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T002 (contracts mục 2.1 → 2.3; chủ dự án duyệt 2026-11-06 API thống kê theo yêu cầu chung của 016
 * module-interfaces mục 2): SPI mua sắm cho M03, API thống kê cho 016, sự kiện công khai, enum theo glossary 25.4.
 */
class T002_AssetProcurementApiContractTest {
    static final String M03 = "com.eldercare.backend.roombedallocation.api.";
    static final String M11 = "com.eldercare.backend.billing.api.";

    @Test
    void T002_interfacesHaveTheirOperations() throws Exception {
        Map<String, List<String>> contracts = Map.of(
                M03 + "spi.ProcurementRequestPort", List.of("checkUsable", "linkSource"),
                M03 + "AssetStatisticsApi", List.of("overdue", "countOverdue", "dueSoon", "vehicleSchedules"),
                M11 + "ProcurementStatisticsApi", List.of("byDepartment", "totals"));
        for (var entry : contracts.entrySet()) {
            Class<?> api = Class.forName(entry.getKey());
            assertThat(api.isInterface()).as(entry.getKey()).isTrue();
            assertThat(Arrays.stream(api.getDeclaredMethods()).map(Method::getName))
                    .as(entry.getKey())
                    .containsExactlyInAnyOrderElementsOf(entry.getValue());
        }
        Method link = Arrays.stream(
                        Class.forName(M03 + "spi.ProcurementRequestPort").getDeclaredMethods())
                .filter(m -> m.getName().equals("linkSource"))
                .findFirst()
                .orElseThrow();
        assertThat(link.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
    }

    @Test
    void T002_eventsKeepTheirFieldsAndCarryNotifications() throws Exception {
        Map<String, List<String>> events = Map.of(
                M03 + "events.AssetStatusChanged",
                List.of("assetId", "departmentId", "bedId", "from", "to", "command", "at", "notificationRequests"),
                M11 + "events.ProcurementRequestChanged",
                List.of("requestId", "departmentId", "type", "from", "to", "command", "at", "notificationRequests"));
        for (var entry : events.entrySet()) {
            Class<?> type = Class.forName(entry.getKey());
            assertThat(type.isRecord()).as(entry.getKey()).isTrue();
            assertThat(HasNotificationRequests.class.isAssignableFrom(type))
                    .as(entry.getKey())
                    .isTrue();
            assertThat(Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName))
                    .as(entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
        }
    }

    private static ProcurementRequestView view(
            long id, long department, ProcurementStatus status, Long approved, Long actual, LocalDate createdOn) {
        return new ProcurementRequestView(
                id, department, ProcurementType.INGREDIENT, status, 2_000_000L, approved, actual, createdOn, null);
    }
}
