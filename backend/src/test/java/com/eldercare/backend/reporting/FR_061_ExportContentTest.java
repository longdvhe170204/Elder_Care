package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.reporting.internal.export.domain.ExportContent;
import com.eldercare.backend.reporting.internal.export.domain.ExportContent.SummaryRow;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.report.domain.MetricResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * FR-061, Q-193, Q-197, Q-198 (016 T025, tên logic {@code FR_061_NoiDungXuatTest}; quickstart Q25, Q26): nội dung file xuất
 * theo vai trò. Chỉ Quản lý viện xuất có định danh người cao tuổi và phải có mục đích; vai trò khác (và Quản lý viện không
 * chọn định danh) chỉ có số liệu tổng hợp, bỏ mọi dòng mà nhóm chỉ có đúng một người cao tuổi; số giờ làm theo nhân viên
 * chỉ Quản lý viện xuất.
 */
class FR_061_ExportContentTest {

    private static final Set<Role> MANAGER = EnumSet.of(Role.NURSING_HOME_MANAGER);
    private static final Set<Role> FLOOR_MANAGER = EnumSet.of(Role.FLOOR_MANAGER);

    private static DetailRow row(String key, long residentId, String resident) {
        return new DetailRow(
                key,
                "006",
                "DOSE",
                "MISSED",
                Instant.parse("2026-10-10T01:00:00Z"),
                1L,
                "Tầng 1",
                residentId,
                resident,
                "Điều dưỡng A",
                Map.of(),
                null,
                Map.of(),
                false,
                false,
                true);
    }

    /** Liều Bỏ lỡ: tầng 1 có hai người (1, 2), tầng 2 chỉ một người (3). */
    private static MetricResult missedDoses() {
        MetricValue value = new MetricValue(
                "MED.MISSED_RATE",
                BigDecimal.valueOf(3),
                null,
                null,
                Set.of(),
                0,
                List.of(
                        new MetricValue.Breakdown("Tầng 1", BigDecimal.valueOf(2), null, null),
                        new MetricValue.Breakdown("Tầng 2", BigDecimal.ONE, null, null)));
        return new MetricResult(
                "MED.MISSED_RATE",
                "Liều Bỏ lỡ",
                value,
                Set.of(1L, 2L, 3L),
                Map.of("Tầng 1", Set.of(1L, 2L), "Tầng 2", Set.of(3L)),
                List.of(row("d-1", 1, "Nguyễn Văn A"), row("d-2", 2, "Trần Thị B"), row("d-3", 3, "Lê Văn C")));
    }

    private static MetricResult workingHours() {
        return new MetricResult(
                "SHIFT.WORKING_HOURS",
                "Số giờ làm",
                new MetricValue("SHIFT.WORKING_HOURS", BigDecimal.TEN, null, null, Set.of(), 0, List.of()),
                Set.of(),
                Map.of(),
                List.of());
    }

    @Test
    void Q26_otherRole_aggregateOnly_dropsSinglePersonGroups_noDetailRows() {
        ExportContent content = ExportContent.decide(FLOOR_MANAGER, false, Optional.empty(), List.of(missedDoses()));

        assertThat(content.includeIdentity()).isFalse();
        assertThat(content.details()).isEmpty();
        assertThat(content.summary()).extracting(SummaryRow::breakdownKey).containsExactly(null, "Tầng 1");
        assertThat(content.summary().getFirst().value()).isEqualByComparingTo("3");
        assertThat(content.rowCount()).isEqualTo(2);
    }

    @Test
    void otherRole_totalOfOnePerson_isDropped() {
        MetricResult one = new MetricResult(
                "MED.MISSED_RATE",
                "Liều Bỏ lỡ",
                new MetricValue("MED.MISSED_RATE", BigDecimal.ONE, null, null, Set.of(), 0, List.of()),
                Set.of(7L),
                Map.of(),
                List.of(row("d-7", 7, "Phạm Văn D")));

        assertThat(ExportContent.decide(FLOOR_MANAGER, false, Optional.empty(), List.of(one))
                        .summary())
                .isEmpty();
    }

    @Test
    void US9_1_otherRole_withIdentity_isRejected() {
        assertThatThrownBy(() -> ExportContent.decide(FLOOR_MANAGER, true, Optional.of("họp"), List.of(missedDoses())))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("chỉ Quản lý viện");
    }

    @Test
    void Q25_manager_withIdentity_withoutPurpose_isRejected() {
        assertThatThrownBy(() -> ExportContent.decide(MANAGER, true, Optional.empty(), List.of(missedDoses())))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("PURPOSE_REQUIRED"));
        assertThatThrownBy(() -> ExportContent.decide(MANAGER, true, Optional.of("  "), List.of(missedDoses())))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("PURPOSE_REQUIRED"));
    }

    @Test
    void Q25_manager_withIdentityAndPurpose_hasEveryRowAndDetails() {
        ExportContent content =
                ExportContent.decide(MANAGER, true, Optional.of("họp giao ban an toàn thuốc"), List.of(missedDoses()));

        assertThat(content.includeIdentity()).isTrue();
        assertThat(content.summary()).extracting(SummaryRow::breakdownKey).containsExactly(null, "Tầng 1", "Tầng 2");
        assertThat(content.details())
                .extracting(d -> d.row().resident())
                .containsExactly("Nguyễn Văn A", "Trần Thị B", "Lê Văn C");
        assertThat(content.rowCount()).isEqualTo(6);
    }

    @Test
    void manager_withoutIdentity_isAggregateOnly() {
        ExportContent content = ExportContent.decide(MANAGER, false, Optional.empty(), List.of(missedDoses()));

        assertThat(content.details()).isEmpty();
        assertThat(content.summary()).extracting(SummaryRow::breakdownKey).containsExactly(null, "Tầng 1");
    }

    @Test
    void Q197_workingHoursPerStaff_onlyManagerExports() {
        assertThat(ExportContent.decide(FLOOR_MANAGER, false, Optional.empty(), List.of(workingHours()))
                        .summary())
                .isEmpty();
        assertThat(ExportContent.decide(MANAGER, false, Optional.empty(), List.of(workingHours()))
                        .summary())
                .extracting(SummaryRow::metricCode)
                .containsExactly("SHIFT.WORKING_HOURS");
    }
}
