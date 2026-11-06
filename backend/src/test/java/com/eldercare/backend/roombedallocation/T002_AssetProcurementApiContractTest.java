package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.dto.ProcurementRequestView;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.api.dto.ProcurementTotalView;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.roombedallocation.api.dto.AssetFlag;
import com.eldercare.backend.roombedallocation.api.dto.OverdueAssetView;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import com.eldercare.backend.support.fake.billing.FakeProcurementStatistics;
import com.eldercare.backend.support.fake.roombedallocation.FakeAssetStatistics;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                M03 + "AssetStatisticsApi", List.of("overdue", "countOverdue", "vehicleSchedules"),
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

    @Test
    void T002_enumsFollowGlossary() throws Exception {
        Map<String, List<String>> enums = Map.of(
                M03 + "dto.ProcurementType",
                List.of("INGREDIENT", "ASSET", "SERVICE", "MEDICATION", "OTHER"),
                M03 + "dto.ProcurementSourceType",
                List.of("ASSET", "MAINTENANCE"),
                M03 + "dto.AssetStatus",
                List.of("READY", "IN_USE", "DAMAGED", "UNDER_MAINTENANCE", "DISPOSED"),
                M03 + "dto.AssetFlag",
                List.of(
                        "MAINTENANCE_OVERDUE",
                        "VEHICLE_INSPECTION_OVERDUE",
                        "INSURANCE_OVERDUE",
                        "AWAITING_RESIDENT_TRANSFER"),
                M03 + "dto.VehicleSchedulePurpose",
                List.of("TRIP", "MEDICAL_VISIT", "NON_EMERGENCY_TRANSFER", "OTHER"),
                M03 + "dto.VehicleScheduleStatus",
                List.of("BOOKED", "IN_USE", "COMPLETED", "CANCELLED"),
                M11 + "dto.ProcurementStatus",
                List.of(
                        "DRAFT",
                        "PENDING_APPROVAL",
                        "APPROVED",
                        "PENDING_RECONCILIATION",
                        "DISCREPANCY",
                        "RECONCILED",
                        "REJECTED",
                        "CANCELLED"));
        for (var entry : enums.entrySet()) {
            Class<?> type = Class.forName(entry.getKey());
            assertThat(Arrays.stream(type.getEnumConstants()).map(Object::toString))
                    .as(entry.getKey())
                    .containsExactlyElementsOf(entry.getValue());
        }
    }

    @Test
    void T002_fakeAssetStatisticsFiltersFloorScopeAndCountsWithoutRows() {
        var day = LocalDate.of(2026, 12, 1);
        var fake = new FakeAssetStatistics()
                .overdue(new OverdueAssetView(1, "XE-01", "Xe", 5, null, AssetFlag.VEHICLE_INSPECTION_OVERDUE, day))
                .overdue(new OverdueAssetView(2, "TS-15", "Máy oxy", 6, 20L, AssetFlag.MAINTENANCE_OVERDUE, day))
                .overdue(new OverdueAssetView(3, "TS-16", "Máy đo", 6, 21L, AssetFlag.MAINTENANCE_OVERDUE, day));
        var floor20 = new ScopeFilter(false, Set.of(20L), Set.of(), Set.of());

        assertThat(fake.overdue(ScopeFilter.FACILITY, day)).as("chưa qua hạn").isEmpty();
        assertThat(fake.overdue(floor20, day.plusDays(1)).map(OverdueAssetView::assetId))
                .containsExactly(2L);
        assertThat(fake.countOverdue(ScopeFilter.FACILITY, day.plusDays(1)))
                .containsEntry(AssetFlag.MAINTENANCE_OVERDUE, 2L)
                .containsEntry(AssetFlag.VEHICLE_INSPECTION_OVERDUE, 1L);
    }

    @Test
    void T002_fakeProcurementStatisticsTotalsByDepartmentAndStatus() {
        var day = LocalDate.of(2026, 10, 1);
        var fake = new FakeProcurementStatistics()
                .request(view(1, 7, ProcurementStatus.RECONCILED, 1_800_000L, 1_950_000L, day))
                .request(view(2, 7, ProcurementStatus.RECONCILED, 500_000L, 500_000L, day.plusDays(3)))
                .request(view(3, 8, ProcurementStatus.PENDING_APPROVAL, null, null, day))
                .request(view(4, 7, ProcurementStatus.RECONCILED, 100L, 100L, day.plusDays(40)));

        assertThat(fake.totals(Set.of(7L), day, day.plusDays(30)))
                .containsExactly(new ProcurementTotalView(7, ProcurementStatus.RECONCILED, 2, 2_300_000L, 2_450_000L));
        assertThat(fake.byDepartment(Set.of(), day, day.plusDays(30)).map(ProcurementRequestView::requestId))
                .containsExactly(1L, 2L, 3L);
    }

    private static ProcurementRequestView view(
            long id, long department, ProcurementStatus status, Long approved, Long actual, LocalDate createdOn) {
        return new ProcurementRequestView(
                id, department, ProcurementType.INGREDIENT, status, 2_000_000L, approved, actual, createdOn, null);
    }
}
