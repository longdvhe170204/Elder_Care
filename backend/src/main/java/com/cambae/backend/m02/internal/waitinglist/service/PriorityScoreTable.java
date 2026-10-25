package com.cambae.backend.m02.internal.waitinglist.service;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.Set;

/**
 * Bảng điểm ưu tiên CFG-M02-08 (FR-009) đọc lúc tính. {@code careLevelPoints}, {@code circumstancePoints} rỗng nghĩa là
 * viện chưa đặt ("Theo cơ sở"): thành phần đó tính 0 điểm. Chu kỳ chờ tính theo ngày; 0 là không cộng điểm chờ.
 */
public record PriorityScoreTable(
        int waitingPeriodDays,
        int pointsPerWaitingPeriod,
        Map<CareLevel, Integer> careLevelPoints,
        Map<String, Integer> circumstancePoints) {

    public int pointsFor(CareLevel level) {
        return careLevelPoints == null ? 0 : careLevelPoints.getOrDefault(level, 0);
    }

    public int pointsFor(String circumstanceCode) {
        return circumstancePoints == null ? 0 : circumstancePoints.getOrDefault(circumstanceCode, 0);
    }

    /** Mã tình huống viện đã đặt điểm; tình huống ngoài tập này không chọn được (FR-007). */
    public Set<String> circumstanceCodes() {
        return circumstancePoints == null ? Set.of() : circumstancePoints.keySet();
    }

    /** Dạng JSON của tham số (kiểu TABLE): {@code waitingPeriod} là thời lượng {@code {value, unit}}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Config(
            Period waitingPeriod,
            Integer pointsPerWaitingPeriod,
            Map<CareLevel, Integer> careLevelPoints,
            Map<String, Integer> specialSituationPoints) {

        public record Period(int value, String unit) {}

        public PriorityScoreTable toTable() {
            int days = 0;
            if (waitingPeriod != null) {
                if (!"DAYS".equals(waitingPeriod.unit())) {
                    throw new IllegalStateException(
                            "CFG-M02-08: chu kỳ chờ tính theo ngày, nhận " + waitingPeriod.unit());
                }
                days = waitingPeriod.value();
            }
            return new PriorityScoreTable(
                    days,
                    pointsPerWaitingPeriod == null ? 0 : pointsPerWaitingPeriod,
                    careLevelPoints,
                    specialSituationPoints);
        }
    }
}
