package com.cambae.backend.m14;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m14.internal.metric.domain.Metric;
import com.cambae.backend.m14.internal.metric.domain.Metric.Computation;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricRecord;
import com.cambae.backend.m14.internal.metric.domain.MetricValue;
import com.cambae.backend.m14.internal.metric.domain.ValueLabel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Khung chỉ tiêu (016 T003; research O-05): FR-007, FR-008, FR-009, FR-015. */
class MetricTest {

    record Task(
            String sourceKey,
            String status,
            boolean mandatory,
            Long floorAreaId,
            boolean voided,
            boolean pendingReview,
            BigDecimal minutes)
            implements MetricRecord {

        @Override
        public Instant referenceTime() {
            return Instant.parse("2026-10-20T01:00:00Z");
        }

        @Override
        public Long residentId() {
            return null;
        }
    }

    private static Task task(String key, String status, long floor) {
        return new Task(key, status, true, floor, false, false, null);
    }

    private static DetailRow row(Task t) {
        return new DetailRow(
                t.sourceKey(),
                "005",
                "TASK",
                t.status(),
                t.referenceTime(),
                t.floorAreaId(),
                null,
                null,
                null,
                null,
                Map.of(),
                null,
                Map.of(),
                t.pendingReview(),
                false,
                true);
    }

    private static final Metric<Task> OVERDUE = new Metric<>(
            "TASK.OVERDUE",
            MetricGroup.CARE_TASKS,
            "Công việc quá hạn",
            "Số công việc đang Quá hạn lúc xem",
            "005",
            new Computation.Count<>(t -> t.status().equals("OVERDUE")),
            t -> String.valueOf(t.floorAreaId()),
            MetricTest::row);

    private static final Metric<Task> ON_TIME = new Metric<>(
            "TASK.MANDATORY_ON_TIME_RATE",
            MetricGroup.CARE_TASKS,
            "Tỷ lệ đúng hạn",
            "Hoàn thành ÷ (Hoàn thành + Hoàn thành trễ + Không thực hiện)",
            "005",
            new Computation.Ratio<>(
                    t -> t.mandatory()
                            && List.of("COMPLETED", "COMPLETED_LATE", "NOT_DONE")
                                    .contains(t.status()),
                    t -> t.status().equals("COMPLETED")),
            null,
            MetricTest::row);

    @Test
    void FR_007_voidedRecordsAreNotCounted_FR_015_duplicatesCountedOnce() {
        Task voided = new Task("t3", "OVERDUE", true, 1L, true, false, null);
        List<Task> included = OVERDUE.included(
                Stream.of(task("t1", "OVERDUE", 1), task("t1", "OVERDUE", 1), task("t2", "OVERDUE", 2), voided),
                t -> true);

        assertThat(OVERDUE.compute(included).value()).isEqualByComparingTo("2");
    }

    @Test
    void FR_009_detailRowsMatchTheNumber_andEachBreakdownCell() {
        List<Task> included = OVERDUE.included(
                Stream.of(
                        task("t1", "OVERDUE", 1),
                        task("t2", "OVERDUE", 2),
                        task("t3", "OVERDUE", 2),
                        task("t4", "COMPLETED", 2)),
                t -> true);

        MetricValue value = OVERDUE.compute(included);

        assertThat(OVERDUE.details(included, null)).hasSize(value.value().intValue());
        assertThat(value.breakdown()).extracting(MetricValue.Breakdown::key).containsExactly("1", "2");
        assertThat(value.breakdown().get(1).value()).isEqualByComparingTo("2");
        assertThat(OVERDUE.details(included, "2"))
                .extracting(DetailRow::sourceKey)
                .containsExactly("t2", "t3");
    }

    @Test
    void FR_013_recordsOutsideScopeAreNotCounted() {
        List<Task> included = OVERDUE.included(
                Stream.of(task("t1", "OVERDUE", 1), task("t2", "OVERDUE", 2)), t -> t.floorAreaId() == 1L);

        assertThat(OVERDUE.compute(included).value()).isEqualByComparingTo("1");
        assertThat(OVERDUE.details(included, null))
                .extracting(DetailRow::sourceKey)
                .containsExactly("t1");
    }

    @Test
    void FR_008_ratioCarriesNumeratorAndDenominator_detailsAreTheDenominator() {
        List<Task> included = ON_TIME.included(
                Stream.of(
                        task("a", "COMPLETED", 1),
                        task("b", "COMPLETED", 1),
                        task("c", "COMPLETED_LATE", 1),
                        task("d", "OVERDUE", 1)),
                t -> true);

        MetricValue value = ON_TIME.compute(included);

        assertThat(value.value()).isEqualByComparingTo("66.7");
        assertThat(value.numerator()).isEqualByComparingTo("2");
        assertThat(value.denominator()).isEqualByComparingTo("3");
        assertThat(value.labels()).isEmpty();
        assertThat(ON_TIME.details(included, null)).hasSize(3);
    }

    @Test
    void FR_008_zeroDenominator_noValueAndNoDataLabel() {
        MetricValue value = ON_TIME.compute(ON_TIME.included(Stream.of(task("d", "OVERDUE", 1)), t -> true));

        assertThat(value.value()).isNull();
        assertThat(value.denominator()).isEqualByComparingTo("0");
        assertThat(value.labels()).containsExactly(ValueLabel.NO_DATA);
    }

    @Test
    void FR_007_pendingReviewRecordsCountedWithLabel() {
        Task pending = new Task("p", "OVERDUE", true, 1L, false, true, null);
        MetricValue value = OVERDUE.compute(OVERDUE.included(Stream.of(task("t1", "OVERDUE", 1), pending), t -> true));

        assertThat(value.value()).isEqualByComparingTo("2");
        assertThat(value.pendingReviewCount()).isEqualTo(1);
        assertThat(value.labels()).containsExactly(ValueLabel.PENDING_REVIEW);
    }

    @Test
    void FR_009_averageDetailsAreTheRecordsInTheCalculation() {
        Metric<Task> average = new Metric<>(
                "X.AVG",
                MetricGroup.CARE_TASKS,
                "TB",
                "TB phút",
                "005",
                new Computation.Aggregate<>(Computation.Aggregate.Kind.AVERAGE, Task::minutes),
                null,
                MetricTest::row);
        List<Task> included = average.included(
                Stream.of(
                        new Task("a", "S", true, 1L, false, false, BigDecimal.valueOf(10)),
                        new Task("b", "S", true, 1L, false, false, BigDecimal.valueOf(25)),
                        new Task("c", "S", true, 1L, false, false, null)),
                t -> true);

        assertThat(average.compute(included).value()).isEqualByComparingTo("17.5");
        assertThat(average.details(included, null))
                .extracting(DetailRow::sourceKey)
                .containsExactly("a", "b");
    }
}
