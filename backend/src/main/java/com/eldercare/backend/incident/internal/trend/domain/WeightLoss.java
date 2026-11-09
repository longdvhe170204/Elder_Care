package com.eldercare.backend.incident.internal.trend.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Hàm thuần "SutCan" (FR-039a, research S-10): so lần cân mới với giá trị hiện hành cao nhất của các lần trước trong
 * cửa sổ; giảm từ ngưỡng phần trăm trở lên thì là sụt cân. Không có lần trước thì không chạy (CHK042).
 */
public final class WeightLoss {
    private WeightLoss() {}

    /** Một lần cân đã xác nhận, giá trị hiện hành sau đính chính. */
    public record Reading(long recordId, BigDecimal value) {}

    /** Kết quả phát hiện: lần cao nhất trong cửa sổ và tỷ lệ giảm (phần trăm, làm tròn 1 chữ số). */
    public record Detection(Reading highest, Reading latest, BigDecimal dropPercent) {}

    public static Optional<Detection> detect(Reading latest, List<Reading> previous, BigDecimal thresholdPercent) {
        return previous.stream()
                .filter(reading -> reading.recordId() != latest.recordId())
                .filter(reading -> reading.value().signum() > 0)
                .max(java.util.Comparator.comparing(Reading::value))
                .flatMap(highest -> {
                    var drop = highest.value()
                            .subtract(latest.value())
                            .multiply(BigDecimal.valueOf(100))
                            .divide(highest.value(), 1, RoundingMode.HALF_UP);
                    return drop.compareTo(thresholdPercent) >= 0
                            ? Optional.of(new Detection(highest, latest, drop))
                            : Optional.empty();
                });
    }
}
