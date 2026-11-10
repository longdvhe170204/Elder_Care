package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.api.ProcurementStatisticsApi;
import com.eldercare.backend.billing.api.dto.ProcurementRequestView;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.api.dto.ProcurementTotalView;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 019 T023, contracts mục 2.3: ProcurementStatisticsApi cho 016 CP.MUA_SAM (FR-055a). Khoảng theo ngày lập (A xác nhận
 * 2026-11-07), hai đầu tính vào; bộ phận và năm 2031 riêng của test nên không phụ thuộc dữ liệu sót.
 */
class T023_ProcurementStatisticsIT extends ProcurementTestBase {
    static final LocalDate FROM = LocalDate.of(2031, 3, 1);
    static final LocalDate TO = LocalDate.of(2031, 3, 31);

    @Autowired
    ProcurementStatisticsApi statistics;

    @Test
    void ngay_lap_trong_khoang_hai_dau_loc_bo_phan_va_tong_theo_trang_thai() {
        long a = TestUsers.nextId(), b = TestUsers.nextId();
        long first = insert(a, "APPROVED", FROM.atStartOfDay(), 2_000_000L, null, null);
        long last = insert(b, "RECONCILED", TO.atTime(LocalTime.of(23, 59, 59)), 1_500_000L, 1_400_000L, TO);
        insert(a, "APPROVED", FROM.minusDays(1).atTime(23, 59, 59), 1L, null, null);
        insert(a, "APPROVED", TO.plusDays(1).atStartOfDay(), 1L, null, null);

        try (var rows = statistics.byDepartment(Set.of(a, b), FROM, TO)) {
            assertThat(rows.toList())
                    .containsExactlyInAnyOrder(
                            new ProcurementRequestView(
                                    first,
                                    a,
                                    ProcurementType.INGREDIENT,
                                    ProcurementStatus.APPROVED,
                                    2_000_000L,
                                    2_000_000L,
                                    null,
                                    FROM,
                                    null),
                            new ProcurementRequestView(
                                    last,
                                    b,
                                    ProcurementType.INGREDIENT,
                                    ProcurementStatus.RECONCILED,
                                    2_000_000L,
                                    1_500_000L,
                                    1_400_000L,
                                    TO,
                                    TO));
        }
        try (var rows = statistics.byDepartment(Set.of(b), FROM, TO)) {
            assertThat(rows.map(ProcurementRequestView::requestId)).containsExactly(last);
        }
        try (var rows = statistics.byDepartment(Set.of(), FROM, TO)) {
            assertThat(rows.map(ProcurementRequestView::requestId)).contains(first, last);
        }

        // totals: đếm, cộng tiền theo (bộ phận, trạng thái); tiền vượt int để kiểm DECIMAL(15,0) đọc đủ thành long.
        long c = TestUsers.nextId(), d = TestUsers.nextId();
        insert(c, "RECONCILED", FROM.atTime(8, 0), 3_000_000_000L, 2_900_000_000L, FROM);
        insert(c, "RECONCILED", TO.atTime(9, 0), 3_000_000_000L, 3_000_000_000L, TO);
        insert(c, "PENDING_APPROVAL", FROM.atTime(10, 0), null, null, null);
        insert(d, "APPROVED", FROM.atTime(11, 0), 500_000L, null, null);
        insert(d, "APPROVED", TO.plusDays(1).atTime(11, 0), 999L, null, null);

        assertThat(statistics.totals(Set.of(c, d), FROM, TO))
                .containsExactlyInAnyOrder(
                        new ProcurementTotalView(c, ProcurementStatus.PENDING_APPROVAL, 1, 0, 0),
                        new ProcurementTotalView(c, ProcurementStatus.RECONCILED, 2, 6_000_000_000L, 5_900_000_000L),
                        new ProcurementTotalView(d, ProcurementStatus.APPROVED, 1, 500_000L, 0));
        assertThat(statistics.totals(Set.of(), FROM, TO))
                .filteredOn(t -> t.departmentId() == c || t.departmentId() == d)
                .hasSize(3);
    }

    /** Yêu cầu nguyên liệu dự trù 2.000.000 đồng, ngày lập theo giờ nghiệp vụ như cột created_at. */
    private long insert(
            long department,
            String status,
            LocalDateTime createdAt,
            Long approved,
            Long actual,
            LocalDate purchasedOn) {
        boolean last = Set.of("RECONCILED", "REJECTED", "CANCELLED").contains(status);
        jdbc.update(
                "INSERT INTO procurement_request (department_id, created_by_staff_id, type, `lines`, estimated_total,"
                        + " reason, linked_sources, status, created_at, approved_amount, actual_amount, purchased_on,"
                        + " locked_at) VALUES (?, ?, 'INGREDIENT', '[]', 2000000, 'T023', '[]', ?, ?, ?, ?, ?, ?)",
                department,
                kitchenLead.staffId(),
                status,
                createdAt,
                approved,
                actual,
                purchasedOn,
                last ? createdAt : null);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
