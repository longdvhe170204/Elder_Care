package com.eldercare.backend.reporting.internal.scope.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.ParameterCode;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khoảng ngày của báo cáo và xuất (FR-005, FR-060): ngày dương lịch theo Asia/Ho_Chi_Minh, gồm cả ngày đầu và ngày cuối;
 * dài hơn CFG-M14-01 thì từ chối và nêu giới hạn hiện hành, không tự cắt.
 */
@Service
@Transactional(readOnly = true)
public class ReportPeriods {

    /** Khoảng đã kiểm; {@code [start, end)} là 00:00 ngày đầu tới 00:00 ngày sau ngày cuối. */
    public record ReportPeriod(LocalDate from, LocalDate to, Instant start, Instant end) {}

    private final ParameterApi parameters;
    private final BusinessTime time;

    public ReportPeriods(ParameterApi parameters, BusinessTime time) {
        this.parameters = parameters;
        this.time = time;
    }

    public ReportPeriod check(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new InvalidValueException("Khoảng thời gian không hợp lệ");
        }
        Period maximum = parameters.read(ParameterCode.CFG_M14_01).asPeriod();
        if (!to.isBefore(from.plus(maximum))) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "RANGE_EXCEEDS_MAXIMUM",
                    "Khoảng thời gian dài hơn giới hạn " + describe(maximum) + " (CFG-M14-01)",
                    Map.of("maximum", maximum.toString()));
        }
        return new ReportPeriod(from, to, time.startOfDay(from), time.startOfDay(to.plusDays(1)));
    }

    private static String describe(Period p) {
        if (p.getYears() > 0 && p.getMonths() == 0 && p.getDays() == 0) {
            return p.getYears() + " năm";
        }
        if (p.getDays() == 0) {
            return p.toTotalMonths() + " tháng";
        }
        return p.getDays() + " ngày";
    }
}
