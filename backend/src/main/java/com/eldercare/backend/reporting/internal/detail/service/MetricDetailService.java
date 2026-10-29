package com.eldercare.backend.reporting.internal.detail.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricSource;
import com.eldercare.backend.reporting.internal.metric.domain.ReportFilters;
import com.eldercare.backend.reporting.internal.metric.service.MetricCatalog;
import com.eldercare.backend.reporting.internal.metric.service.MetricEvaluator;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope;
import com.eldercare.backend.reporting.internal.scope.service.ReportPeriods;
import com.eldercare.backend.reporting.internal.scope.service.ReportPeriods.ReportPeriod;
import com.eldercare.backend.reporting.internal.scope.service.ReportScopeService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách bản ghi tạo nên một con số ({@code ChiTietService}; FR-009, FR-012, FR-014; 016 T022 phần khung): cùng phạm vi,
 * bộ lọc với con số; có khoảng thì như báo cáo, không có thì như dashboard (hôm nay, phạm vi hiện hành). Dòng người cao
 * tuổi ngoài phạm vi hiện hành ở dạng rút gọn (Q-196). Mốc "tính đến" chưa dùng: nguồn chưa có số liệu tại một thời điểm.
 */
@Service
@Transactional(readOnly = true)
public class MetricDetailService {

    /** Số dòng mỗi trang (thông số kỹ thuật, không phải CFG). */
    public static final int PAGE_SIZE = 50;

    public record MetricDetail(String metricCode, int total, List<DetailRow> rows) {}

    private final MetricCatalog catalog;
    private final ReportScopeService scopes;
    private final ReportPeriods periods;
    private final MetricEvaluator evaluator;
    private final BusinessTime time;

    public MetricDetailService(
            MetricCatalog catalog,
            ReportScopeService scopes,
            ReportPeriods periods,
            MetricEvaluator evaluator,
            BusinessTime time) {
        this.catalog = catalog;
        this.scopes = scopes;
        this.periods = periods;
        this.evaluator = evaluator;
        this.time = time;
    }

    public MetricDetail details(
            CurrentUser user, String metricCode, LocalDate from, LocalDate to, String breakdownKey, int page) {
        MetricSource<?> source = catalog.source(metricCode).orElseThrow(NotFoundException::new);
        List<DetailRow> rows;
        if (from == null && to == null) {
            LocalDate today = time.today();
            ReportScope scope = scopes.dashboard(user, source.metric().group());
            rows = evaluator.currentDetails(
                    user, source, scope, time.startOfDay(today), time.startOfDay(today.plusDays(1)), breakdownKey);
        } else {
            ReportPeriod period = periods.check(from, to);
            ReportScope scope = scopes.period(user, source.metric().group(), period.start(), period.end());
            rows = evaluator.details(
                    user, source, scope, period.start(), period.end(), ReportFilters.NONE, breakdownKey);
        }
        int first = Math.min(rows.size(), (Math.max(page, 1) - 1) * PAGE_SIZE);
        return new MetricDetail(metricCode, rows.size(), rows.subList(first, Math.min(rows.size(), first + PAGE_SIZE)));
    }
}
