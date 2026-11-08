package com.eldercare.backend.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Kiểm một bảng dữ liệu trong một test (AGENTS.md mục 4, ngân sách test tính theo số Maven báo): chạy {@code check} cho
 * từng dòng, gom mọi dòng sai rồi báo một lần, thay cho {@code @ParameterizedTest} mà mỗi dòng thành một ca riêng.
 */
public final class TableRows {

    @FunctionalInterface
    public interface Check<T> {
        void accept(T row) throws Exception;
    }

    private TableRows() {}

    public static <T> void each(Stream<T> rows, Check<T> check) {
        List<String> failures = new ArrayList<>();
        int[] count = {0};
        rows.forEach(row -> {
            count[0]++;
            try {
                check.accept(row);
            } catch (AssertionError | Exception e) {
                failures.add(describe(row) + " → " + e.getMessage());
            }
        });
        if (count[0] == 0) throw new AssertionError("Bảng dữ liệu rỗng");
        if (!failures.isEmpty())
            throw new AssertionError(failures.size() + "/" + count[0] + " dòng sai:\n" + String.join("\n", failures));
    }

    public static <T> void each(List<T> rows, Check<T> check) {
        each(rows.stream(), check);
    }

    /** Dòng CSV như {@code @CsvSource}: tách theo dấu phẩy, bỏ khoảng trắng hai đầu, ô rỗng là {@code null}. */
    public static void csv(Check<String[]> check, String... lines) {
        each(Arrays.stream(lines).map(TableRows::cells), check);
    }

    private static String[] cells(String line) {
        return Arrays.stream(line.split(",", -1))
                .map(String::strip)
                .map(cell -> cell.isEmpty() ? null : cell)
                .toArray(String[]::new);
    }

    private static String describe(Object row) {
        return row instanceof Object[] array ? Arrays.toString(array) : String.valueOf(row);
    }
}
