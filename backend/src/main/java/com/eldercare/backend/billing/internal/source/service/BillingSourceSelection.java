package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.billing.internal.source.domain.IndexedSourceMatch;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Xác định đơn vị nguồn; lệnh lập khoản phải kiểm lại điều kiện dưới khóa của T011. */
@Service
public class BillingSourceSelection {
    private final BillingSourceIndexStore store;

    public BillingSourceSelection(BillingSourceIndexStore store) {
        this.store = store;
    }

    public record SourceUnit(String sourceId, String billingUnit) {}

    public record SkippedSource(SourceUnit source, String errorCode, String reason) {}

    public record Selection(List<IndexedSourceMatch> selected, List<SkippedSource> skipped) {
        public Selection {
            selected = List.copyOf(selected);
            skipped = List.copyOf(skipped);
        }
    }

    public Selection byIds(long residentId, String sourceType, List<String> sourceIds) {
        var ids = sourceIds.stream().distinct().toList();
        var matches = store.findBySourceIds(residentId, sourceType, ids).stream()
                .collect(Collectors.groupingBy(row -> row.source().sourceId()));
        var selected = new ArrayList<IndexedSourceMatch>();
        var skipped = new ArrayList<SkippedSource>();
        for (String id : ids) {
            var candidates = matches.getOrDefault(id, List.of());
            var key = new SourceUnit(id, null);
            if (candidates.isEmpty()) {
                skipped.add(missing(key));
            } else if (candidates.size() > 1) {
                skipped.add(new SkippedSource(
                        key, "CONDITION_NOT_MET", "Nguồn có nhiều đơn vị tính phí; cần chọn đơn vị cụ thể."));
            } else {
                selected.add(candidates.getFirst());
            }
        }
        return new Selection(selected, skipped);
    }

    public Selection byUnits(long residentId, String sourceType, List<SourceUnit> sourceUnits) {
        var units = sourceUnits.stream().distinct().toList();
        var ids = units.stream().map(SourceUnit::sourceId).distinct().toList();
        var matches = store.findBySourceIds(residentId, sourceType, ids).stream()
                .collect(Collectors.toMap(
                        row -> new SourceUnit(
                                row.source().sourceId(), row.source().unit()),
                        Function.identity()));
        var selected = new ArrayList<IndexedSourceMatch>();
        var skipped = new ArrayList<SkippedSource>();
        for (var unit : units) {
            var match = matches.get(unit);
            if (match == null) {
                skipped.add(missing(unit));
            } else {
                selected.add(match);
            }
        }
        return new Selection(selected, skipped);
    }

    private SkippedSource missing(SourceUnit source) {
        return new SkippedSource(source, "NOT_A_BILLABLE_EVENT", "Không tìm thấy đơn vị nguồn trong phạm vi đã chọn.");
    }
}
