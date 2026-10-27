package com.cambae.backend.m02.internal.registration.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Tổng thời gian lưu trú dự kiến ({@code TongThoiGianLuuTru}; FR-001, FR-001a, FR-007; BR-M02-11, Q-215, Q-241, Q-336;
 * research Z-03), hàm thuần: chuỗi hợp đồng nối tiếp không gián đoạn mới nhất của hồ sơ (hợp đồng sau bắt đầu ngay ngày
 * sau ngày kết thúc hợp đồng trước; có khoảng trống thì tính lại từ hợp đồng sau), từ ngày bắt đầu hợp đồng đầu chuỗi (hoặc
 * ngày hiệu lực phần nội trú nếu muộn hơn) tới ngày kết thúc dự kiến mới nhất; không có ngày kết thúc là vô hạn. Đếm cả
 * ngày đầu và ngày cuối (chủ dự án chốt 2026-10-27).
 *
 * <p>Bên gọi truyền hợp đồng của hồ sơ hiện hành (hồ sơ mới theo Q-12 tính lại từ đầu) với ngày kết thúc dự kiến đã gồm
 * phụ lục gia hạn. Hợp đồng không chồng nhau (DBR-06 của 004); dữ liệu chồng là sai nên bị từ chối, không tự hợp nhất.
 *
 * @param to {@code null} khi hợp đồng cuối chuỗi không có ngày kết thúc
 */
public record ExpectedStayDuration(LocalDate from, LocalDate to) {

    /**
     * @param residentialFrom ngày hiệu lực phần nội trú khi người bán trú chuyển nội trú (FR-001a), {@code null} nếu không
     */
    public static ExpectedStayDuration of(List<StayPeriod> contracts, LocalDate residentialFrom) {
        if (contracts.isEmpty()) {
            throw new IllegalArgumentException("Không có hợp đồng để tính thời gian lưu trú");
        }
        List<StayPeriod> sorted = contracts.stream()
                .sorted(Comparator.comparing(StayPeriod::start))
                .toList();
        LocalDate chainStart = sorted.getFirst().start();
        StayPeriod previous = sorted.getFirst();
        for (StayPeriod next : sorted.subList(1, sorted.size())) {
            if (previous.end() == null || !next.start().isAfter(previous.end())) {
                throw new IllegalArgumentException("Hợp đồng chồng nhau: " + previous + ", " + next);
            }
            if (!next.start().equals(previous.end().plusDays(1))) {
                chainStart = next.start();
            }
            previous = next;
        }
        LocalDate from = chainStart;
        if (residentialFrom != null && residentialFrom.isAfter(chainStart)) {
            if (previous.end() != null && residentialFrom.isAfter(previous.end())) {
                throw new IllegalArgumentException("Phần nội trú hiệu lực sau ngày kết thúc dự kiến");
            }
            from = residentialFrom;
        }
        return new ExpectedStayDuration(from, previous.end());
    }

    /** Số ngày, rỗng khi vô hạn. */
    public OptionalLong days() {
        return to == null ? OptionalLong.empty() : OptionalLong.of(ChronoUnit.DAYS.between(from, to) + 1);
    }

    /** Một hợp đồng: ngày bắt đầu, ngày kết thúc dự kiến gồm gia hạn ({@code null} nếu không có). */
    public record StayPeriod(LocalDate start, LocalDate end) {

        public StayPeriod {
            Objects.requireNonNull(start, "start");
            if (end != null && end.isBefore(start)) {
                throw new IllegalArgumentException("Ngày kết thúc trước ngày bắt đầu: " + start + " → " + end);
            }
        }
    }
}
