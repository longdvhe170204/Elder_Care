package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.care.api.PublicCareResultApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T039: 012 kiểm quyền sức khỏe/ghi nhật ký xem trước khi gọi; không gửi ghi chú nội bộ. */
@Service
@Transactional(readOnly = true)
public class PublicCareResults implements PublicCareResultApi {
    private final CareResultStore store;
    private final CorrectionApi corrections;

    public PublicCareResults(CareResultStore store, CorrectionApi corrections) {
        this.store = store;
        this.corrections = corrections;
    }

    @Override
    public List<FamilyCareResultView> results(long residentId, Instant from, Instant to) {
        var rows = store.history(residentId, from, to);
        var current = corrections.current(
                "M04.CARE_RESULT", rows.stream().map(r -> r.id()).toList());
        var result = new ArrayList<FamilyCareResultView>();
        for (var row : rows) {
            var changed = current.get(row.id());
            if (changed != null && changed.voided()) continue;
            Map<?, ?> values = row.values();
            if (changed != null && changed.corrected() && changed.content() != null) {
                if (!(changed.content() instanceof Map<?, ?> map))
                    throw unavailable("Chưa đọc được nội dung đính chính kết quả");
                values = map;
            }
            var visible = new LinkedHashMap<String, Object>();
            for (var key : List.of("numericValue", "codeValue", "jsonValue"))
                if (values.containsKey(key)) visible.put(key, values.get(key));
            result.add(new FamilyCareResultView(
                    row.id(),
                    residentId,
                    ResultType.valueOf(row.resultType()),
                    row.performedAt(),
                    Collections.unmodifiableMap(visible)));
        }
        return List.copyOf(result);
    }

    @Override
    public EatingDrinkingSummary summary(long residentId, LocalDate from, LocalDate to) {
        // Chủ dự án chốt 2026-11-12: trung bình theo ngày có ít nhất một lần ghi nước, ngày không ghi không tính
        var perDay = new TreeMap<LocalDate, BigDecimal>();
        for (var r : results(
                residentId,
                from.atStartOfDay(BusinessTime.ZONE).toInstant(),
                to.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant())) {
            if (r.resultType() != ResultType.ML || !(r.values().get("numericValue") instanceof Number ml)) continue;
            perDay.merge(
                    LocalDate.ofInstant(r.performedAt(), BusinessTime.ZONE),
                    new BigDecimal(ml.toString()),
                    BigDecimal::add);
        }
        if (perDay.isEmpty()) return new EatingDrinkingSummary(Optional.empty(), 0);
        var total = perDay.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EatingDrinkingSummary(
                Optional.of(total.divide(BigDecimal.valueOf(perDay.size()), 0, RoundingMode.HALF_UP)), perDay.size());
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", message);
    }
}
