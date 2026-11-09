package com.eldercare.backend.nutrition.internal.menu.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Hàm thuần cảnh báo lặp món (FR-026, BR-M08-07, CFG-M08-03; research M-07): cùng món chính, cùng chế độ ăn, xuất hiện
 * từ {@code threshold} ngày liên tiếp trở lên, không phân biệt bữa; chuỗi tính qua ranh giới tuần với thực đơn Công bố, Đã
 * áp dụng liền trước, liền sau. Món thay thế không tính. Chỉ báo chuỗi có ít nhất một ngày thuộc tuần đang kiểm tra.
 */
public final class DishRepetitionCheck {

    public record Repetition(long dietId, long dishId, LocalDate from, LocalDate to) {

        /** Mã cảnh báo cần xác nhận (FR-025): cùng chế độ ăn, món, chuỗi ngày là cùng một cảnh báo. */
        public String code() {
            return "REPETITION:" + dietId + ":" + dishId + ":" + from + ":" + to;
        }
    }

    private record Key(long dietId, long dishId) {}

    private DishRepetitionCheck() {}

    public static List<Repetition> find(
            LocalDate weekStart, List<MenuDish> week, List<MenuDish> neighbours, int threshold) {
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<Key, TreeSet<LocalDate>> days = new LinkedHashMap<>();
        Stream.concat(week.stream(), neighbours.stream())
                .forEach(d -> days.computeIfAbsent(new Key(d.dietId(), d.dishId()), k -> new TreeSet<>())
                        .add(d.date()));
        List<Repetition> out = new ArrayList<>();
        days.forEach((key, dates) -> {
            LocalDate start = null;
            LocalDate previous = null;
            for (LocalDate date : dates) {
                if (previous == null || !date.equals(previous.plusDays(1))) {
                    add(out, key, start, previous, threshold, weekStart, weekEnd);
                    start = date;
                }
                previous = date;
            }
            add(out, key, start, previous, threshold, weekStart, weekEnd);
        });
        return out;
    }

    private static void add(
            List<Repetition> out,
            Key key,
            LocalDate from,
            LocalDate to,
            int threshold,
            LocalDate weekStart,
            LocalDate weekEnd) {
        if (from == null
                || from.plusDays(threshold - 1L).isAfter(to)
                || to.isBefore(weekStart)
                || from.isAfter(weekEnd)) {
            return;
        }
        out.add(new Repetition(key.dietId(), key.dishId(), from, to));
    }
}
