package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.reporting.internal.export.domain.ExportContent;
import com.eldercare.backend.reporting.internal.export.domain.ExportContent.SummaryRow;
import com.eldercare.backend.reporting.internal.export.domain.ExportWorkbook;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricValue;
import com.eldercare.backend.reporting.internal.metric.domain.ValueLabel;
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

    @Test
    void Q26_otherRole_aggregateOnly_dropsSinglePersonGroups_noDetailRows() {
        ExportContent content = ExportContent.decide(FLOOR_MANAGER, false, Optional.empty(), List.of(missedDoses()));

        assertThat(content.includeIdentity()).isFalse();
        assertThat(content.details()).isEmpty();
        assertThat(content.summary()).extracting(SummaryRow::breakdownKey).containsExactly(null, "Tầng 1");
        assertThat(content.summary().getFirst().value()).isEqualByComparingTo("3");
        assertThat(content.rowCount()).isEqualTo(2);
    }

    /** 016 T109 (FR-007): file xuất ghi nhãn dữ liệu đã loại bỏ kèm loại dữ liệu. */
    @Test
    void FR_007_purgedLabel_notesListDataTypes() {
        MetricResult purged = new MetricResult(
                "MED.MISSED_RATE",
                "Liều Bỏ lỡ",
                new MetricValue(
                        "MED.MISSED_RATE",
                        BigDecimal.valueOf(5),
                        null,
                        null,
                        Set.of(ValueLabel.PURGED_BY_RETENTION),
                        0,
                        List.of(),
                        List.of("M07.DOSE")),
                Set.of(1L, 2L),
                Map.of(),
                List.of());

        SummaryRow total = ExportContent.decide(MANAGER, false, Optional.empty(), List.of(purged))
                .summary()
                .getFirst();
        assertThat(total.purgedDataTypes()).containsExactly("M07.DOSE");
        assertThat(ExportWorkbook.notes(total)).isEqualTo("số liệu không đầy đủ do đã loại bỏ theo lưu trữ (M07.DOSE)");
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
}
