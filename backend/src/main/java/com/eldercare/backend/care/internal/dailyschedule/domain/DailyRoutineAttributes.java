package com.eldercare.backend.care.internal.dailyschedule.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Mốc sinh hoạt cố định (005 FR-056, Q-416): mỗi lần lập, sửa, ngừng thêm một phiên bản áp dụng từ ngày hôm sau; ngày D đọc
 * phiên bản mới nhất có {@code from} ≤ D. Sửa nhiều lần trong một ngày chỉ giữ lần cuối.
 */
public record DailyRoutineAttributes(List<Version> versions) {
    public DailyRoutineAttributes {
        versions = versions == null ? List.of() : List.copyOf(versions);
    }

    public record Version(LocalDate from, String name, LocalTime time, boolean active) {}

    public Optional<Version> on(LocalDate date) {
        return versions.stream().filter(v -> !v.from().isAfter(date)).max(Comparator.comparing(Version::from));
    }

    /** Phiên bản mới thay phiên bản cùng ngày áp dụng (nếu có). */
    public DailyRoutineAttributes with(Version next) {
        var result = new ArrayList<Version>();
        for (var v : versions) if (!v.from().equals(next.from())) result.add(v);
        result.add(next);
        result.sort(Comparator.comparing(Version::from));
        return new DailyRoutineAttributes(result);
    }

    /** Phiên bản mới nhất, kể cả phiên bản chưa tới ngày áp dụng. */
    public Optional<Version> latest() {
        return versions.stream().max(Comparator.comparing(Version::from));
    }
}
