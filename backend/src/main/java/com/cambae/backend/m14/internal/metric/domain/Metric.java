package com.cambae.backend.m14.internal.metric.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Định nghĩa một chỉ tiêu, cố định trong mã ({@code ChiTieu}; FR-003, research O-05). Hàm tính {@link #compute} và hàm
 * chi tiết {@link #details} chạy trên cùng tập {@link #included} nên số dòng chi tiết bằng con số (FR-009); trung bình,
 * lớn nhất, tổng thì danh sách là các bản ghi đưa vào phép tính.
 *
 * @param breakdownKey khóa chia nhóm (trạng thái, tầng, nhân viên…); rỗng là không chia
 */
public record Metric<R extends MetricRecord>(
        String code,
        MetricGroup group,
        String name,
        String definition,
        String source,
        Computation<R> computation,
        Function<? super R, String> breakdownKey,
        Function<? super R, DetailRow> toRow) {

    /** Phép tính của chỉ tiêu. */
    public sealed interface Computation<R> {

        /** Số bản ghi thỏa {@code counted}. */
        record Count<R>(Predicate<? super R> counted) implements Computation<R> {}

        /** Tỷ lệ: tử số là bản ghi thỏa cả hai điều kiện, mẫu số là bản ghi thỏa {@code denominator}. */
        record Ratio<R>(Predicate<? super R> denominator, Predicate<? super R> numerator) implements Computation<R> {}

        /** Trung bình, lớn nhất, tổng của giá trị; bản ghi không có giá trị không vào phép tính. */
        record Aggregate<R>(Kind kind, Function<? super R, BigDecimal> value) implements Computation<R> {
            public enum Kind {
                AVERAGE,
                MAXIMUM,
                SUM
            }
        }
    }

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /** Bản ghi vào chỉ tiêu: bỏ Hủy ghi nhận (FR-007), ngoài phạm vi (FR-013), trùng khóa (FR-015). */
    public List<R> included(Stream<? extends R> records, Predicate<? super R> inScope) {
        Set<String> seen = new HashSet<>();
        List<R> result = new ArrayList<>();
        records.filter(r -> !r.voided())
                .filter(inScope)
                .filter(r -> seen.add(r.sourceKey()))
                .forEach(result::add);
        return result;
    }

    public MetricValue compute(List<R> included) {
        List<R> contributing = contributing(included);
        Value total = value(contributing);
        List<MetricValue.Breakdown> breakdown = new ArrayList<>();
        if (breakdownKey != null) {
            Map<String, List<R>> byKey = new TreeMap<>();
            for (R r : included) {
                byKey.computeIfAbsent(Objects.requireNonNull(breakdownKey.apply(r)), k -> new ArrayList<>())
                        .add(r);
            }
            byKey.forEach((key, rows) -> {
                Value v = value(contributing(rows));
                breakdown.add(new MetricValue.Breakdown(key, v.value, v.numerator, v.denominator));
            });
        }
        int pending =
                (int) contributing.stream().filter(MetricRecord::pendingReview).count();
        Set<ValueLabel> labels = EnumSet.noneOf(ValueLabel.class);
        if (total.value == null) {
            labels.add(ValueLabel.NO_DATA);
        }
        if (pending > 0) {
            labels.add(ValueLabel.PENDING_REVIEW);
        }
        return new MetricValue(
                code, total.value, total.numerator, total.denominator, labels, pending, List.copyOf(breakdown));
    }

    /** Danh sách chi tiết của con số, hoặc của một ô {@code breakdownKey} (rỗng là cả con số). */
    public List<DetailRow> details(List<R> included, String key) {
        return contributing(included).stream()
                .filter(r -> key == null || (breakdownKey != null && key.equals(breakdownKey.apply(r))))
                .map(toRow)
                .toList();
    }

    /**
     * Người cao tuổi khác nhau của các bản ghi tạo nên con số, hoặc của một ô {@code key} (rỗng là cả con số); file xuất
     * không định danh bỏ dòng mà nhóm chỉ có đúng một người (FR-061).
     */
    public Set<Long> residents(List<R> included, String key) {
        Set<Long> result = new HashSet<>();
        for (R r : contributing(included)) {
            if (r.residentId() != null
                    && (key == null || (breakdownKey != null && key.equals(breakdownKey.apply(r))))) {
                result.add(r.residentId());
            }
        }
        return result;
    }

    private List<R> contributing(List<R> rows) {
        Predicate<R> keep = switch (computation) {
            case Computation.Count<R> c -> c.counted()::test;
            case Computation.Ratio<R> r -> r.denominator()::test;
            case Computation.Aggregate<R> a -> x -> a.value().apply(x) != null;
        };
        return rows.stream().filter(keep).toList();
    }

    private record Value(BigDecimal value, BigDecimal numerator, BigDecimal denominator) {}

    private Value value(List<R> contributing) {
        return switch (computation) {
            case Computation.Count<R> c -> new Value(BigDecimal.valueOf(contributing.size()), null, null);
            case Computation.Ratio<R> r -> {
                long numerator = contributing.stream().filter(r.numerator()).count();
                BigDecimal den = BigDecimal.valueOf(contributing.size());
                BigDecimal num = BigDecimal.valueOf(numerator);
                BigDecimal v =
                        contributing.isEmpty() ? null : num.multiply(HUNDRED).divide(den, 1, RoundingMode.HALF_UP);
                yield new Value(v, num, den);
            }
            case Computation.Aggregate<R> a -> {
                List<BigDecimal> values =
                        contributing.stream().map(x -> a.value().apply(x)).toList();
                BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal v = switch (a.kind()) {
                    case SUM -> sum;
                    case MAXIMUM -> values.stream().max(BigDecimal::compareTo).orElse(null);
                    case AVERAGE ->
                        values.isEmpty()
                                ? null
                                : sum.divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
                };
                yield new Value(v, null, null);
            }
        };
    }
}
