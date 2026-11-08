package com.eldercare.backend.support.fake.billing;

import com.eldercare.backend.billing.api.ProcurementStatisticsApi;
import com.eldercare.backend.billing.api.dto.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 019 khai, chủ fake là C: yêu cầu mua sắm cho 016 (FR-055a). Lọc bộ phận (rỗng là toàn viện) và ngày lập; tổng
 * cộng số được duyệt, thực tế (rỗng tính 0). Không mô phỏng quyền thật.
 */
public class FakeProcurementStatistics implements ProcurementStatisticsApi {
    private final List<ProcurementRequestView> rows = new ArrayList<>();

    public FakeProcurementStatistics request(ProcurementRequestView row) {
        rows.add(row);
        return this;
    }

    @Override
    public Stream<ProcurementRequestView> byDepartment(Set<Long> departmentIds, LocalDate from, LocalDate to) {
        return rows.stream()
                .filter(r -> departmentIds.isEmpty() || departmentIds.contains(r.departmentId()))
                .filter(r -> !r.createdOn().isBefore(from) && !r.createdOn().isAfter(to));
    }

    @Override
    public List<ProcurementTotalView> totals(Set<Long> departmentIds, LocalDate from, LocalDate to) {
        record Key(long departmentId, ProcurementStatus status) {}
        Map<Key, List<ProcurementRequestView>> groups = byDepartment(departmentIds, from, to)
                .collect(Collectors.groupingBy(
                        r -> new Key(r.departmentId(), r.status()), LinkedHashMap::new, Collectors.toList()));
        return groups.entrySet().stream()
                .map(e -> new ProcurementTotalView(
                        e.getKey().departmentId(),
                        e.getKey().status(),
                        e.getValue().size(),
                        e.getValue().stream()
                                .mapToLong(r -> Objects.requireNonNullElse(r.approvedAmount(), 0L))
                                .sum(),
                        e.getValue().stream()
                                .mapToLong(r -> Objects.requireNonNullElse(r.actualAmount(), 0L))
                                .sum()))
                .toList();
    }

    public void reset() {
        rows.clear();
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FakeProcurementStatistics fakeProcurementStatistics() {
            return new FakeProcurementStatistics();
        }
    }
}
