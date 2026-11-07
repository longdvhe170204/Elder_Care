package com.eldercare.backend.belongings.internal.inventorycheck.domain;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Kỳ kiểm kê đồ có giá trị tính từ CFG-M12-04 ({@code ma_ky}, {@code han}; 013 T025, FR-028a, FR-028c, BR-M12-07,
 * research K-10, DBR-25): chu kỳ theo tháng hoặc ngày, kỳ bắt đầu ở bội của chu kỳ tính từ gốc lịch; hạn hoàn thành là
 * hết ngày thứ {@code deadlineDays} của kỳ, quá hạn khi ngày hiện tại sau ngày đó.
 */
public record InventoryPeriod(String code, LocalDate start, LocalDate dueDate) {

    /** Chu kỳ của CFG-M12-04: {@code unit} là {@code MONTHS} hoặc {@code DAYS}. */
    public static InventoryPeriod of(LocalDate today, int cycleValue, String cycleUnit, int deadlineDays) {
        if (cycleValue < 1 || deadlineDays < 1) {
            throw new IllegalArgumentException("Chu kỳ và hạn kiểm kê phải lớn hơn 0");
        }
        LocalDate start;
        String code;
        switch (cycleUnit) {
            case "MONTHS" -> {
                long index = today.getYear() * 12L + (today.getMonthValue() - 1);
                long startIndex = Math.floorDiv(index, cycleValue) * cycleValue;
                start = LocalDate.of((int) Math.floorDiv(startIndex, 12), (int) Math.floorMod(startIndex, 12) + 1, 1);
                code = YearMonth.from(start).toString();
            }
            case "DAYS" -> {
                start = LocalDate.ofEpochDay(Math.floorDiv(today.toEpochDay(), cycleValue) * cycleValue);
                code = start.toString();
            }
            default ->
                throw new IllegalArgumentException("Đơn vị chu kỳ kiểm kê phải là MONTHS hoặc DAYS: " + cycleUnit);
        }
        return new InventoryPeriod(code, start, start.plusDays(deadlineDays - 1L));
    }

    /** Quá hạn khi ngày hiện tại sau ngày hạn. */
    public boolean overdueOn(LocalDate today) {
        return today.isAfter(dueDate);
    }
}
