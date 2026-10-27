package com.cambae.backend.m15.retention.internal.service;

import com.cambae.backend.common.scheduling.SchedulerRuns;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider.DataRef;
import com.cambae.backend.m15.retention.internal.service.AttachedRetention.Attached;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/**
 * Một đợt loại bỏ dữ liệu hết hạn lưu giữ (000 T071, research R-11; FR-050, FR-051, FR-051a, BR-M15-12, BR-M15-13).
 *
 * <ol>
 *   <li>Mốc hạn của từng loại tính từ CFG-M15-03, CFG-M15-04 (hoặc tham số riêng) đọc lúc chạy.
 *   <li>Mỗi bản ghi đã hết hạn của một provider là gốc của một chuỗi: thêm mọi bản ghi trỏ tới nó (lặp lại), rồi đính
 *       chính và nhật ký đi kèm từng bản ghi.
 *   <li>Chuỗi còn phần tử chưa hết hạn (hoặc thuộc loại không có provider) được giữ nguyên (FR-051a).
 *   <li>Mỗi chuỗi xóa trong một transaction trên kết nối {@code eldercare_retention}; lỗi ở một chuỗi thì dừng đợt, các
 *       chuỗi đã xóa vẫn giữ kết quả (BR-M15-13).
 *   <li>Luôn ghi một bản tóm tắt với số đếm thực tế, ngoài transaction, người thực hiện là Bộ lập lịch (FR-051).
 * </ol>
 */
@Service
public class ExpiredDataPurge {

    static final String JOB = "PURGE_EXPIRED_DATA";
    static final String RUN_ENTITY = "M15.PURGE_RUN";

    /** Số ứng viên tối đa mỗi loại mỗi đợt (thông số kỹ thuật); phần còn lại thuộc đợt sau. */
    static final int BATCH = 1000;

    private static final Logger LOG = LoggerFactory.getLogger(ExpiredDataPurge.class);

    private final ObjectProvider<RetentionProvider> providers;
    private final RetentionDatabase database;
    private final ParameterApi parameters;
    private final BusinessTime time;
    private final SchedulerRuns runs;
    private final AuditLogApi auditLog;

    ExpiredDataPurge(
            ObjectProvider<RetentionProvider> providers,
            RetentionDatabase database,
            ParameterApi parameters,
            BusinessTime time,
            SchedulerRuns runs,
            AuditLogApi auditLog) {
        this.providers = providers;
        this.database = database;
        this.parameters = parameters;
        this.time = time;
        this.runs = runs;
        this.auditLog = auditLog;
    }

    /** Chạy một đợt, trả id lần chạy ({@code scheduler_run}). */
    public long run() {
        long runId = runs.start(JOB);
        Tally tally = new Tally();
        Map<ParameterCode, JsonNode> applied = new EnumMap<>(ParameterCode.class);
        boolean stopped = false;
        try {
            purge(tally, applied);
        } catch (RuntimeException e) {
            stopped = true;
            LOG.warn("Đợt loại bỏ dữ liệu {} dừng giữa chừng", runId, e);
        }
        auditLog.writeOutsideTransaction(AuditLogEntry.builder()
                .action(AuditAction.EXPIRED_DATA_PURGED)
                .entity(RUN_ENTITY, runId)
                .basis(new Basis(codes(applied), null, null, runId))
                .details(Map.of("deleted", tally.summary(), "parameters", byCode(applied), "stopped", stopped))
                .build());
        runs.finish(runId, stopped ? SchedulerRuns.Result.FAILED : SchedulerRuns.Result.SUCCEEDED, tally.total());
        return runId;
    }

    private void purge(Tally tally, Map<ParameterCode, JsonNode> applied) {
        List<RetentionProvider> all = providers.orderedStream().toList();
        Map<String, RetentionProvider> byType =
                all.stream().collect(Collectors.toMap(RetentionProvider::dataType, p -> p));
        List<AttachedRetention> attached = all.stream()
                .filter(AttachedRetention.class::isInstance)
                .map(AttachedRetention.class::cast)
                .toList();
        Map<String, Instant> cutoffs = new HashMap<>();
        for (RetentionProvider provider : all) {
            cutoffs.put(provider.dataType(), cutoff(provider.retentionKind(), applied));
        }
        Map<String, Map<Long, Instant>> candidates = new LinkedHashMap<>();
        for (RetentionProvider provider : all) {
            if (!(provider instanceof AttachedRetention)) {
                candidates.put(
                        provider.dataType(),
                        provider.expiredCandidates(cutoffs.get(provider.dataType()), BATCH)
                                .collect(Collectors.toMap(
                                        RetentionProvider.Candidate::id,
                                        RetentionProvider.Candidate::at,
                                        (a, b) -> a,
                                        LinkedHashMap::new)));
            }
        }
        Set<DataRef> deleted = new HashSet<>();
        candidates.forEach((type, ids) -> ids.keySet().forEach(id -> {
            DataRef root = new DataRef(type, id);
            if (deleted.contains(root)) {
                return;
            }
            chain(root, byType, candidates, attached, cutoffs).ifPresent(chain -> {
                database.transaction().executeWithoutResult(status -> chain.delete(byType));
                deleted.addAll(chain.records());
                tally.add(chain, candidates);
            });
        }));
    }

    private Instant cutoff(RetentionKind kind, Map<ParameterCode, JsonNode> applied) {
        var value = parameters.read(kind.parameter());
        applied.put(kind.parameter(), value.raw());
        return time.now().atZone(BusinessTime.ZONE).minus(value.asPeriod()).toInstant();
    }

    private static Optional<Chain> chain(
            DataRef root,
            Map<String, RetentionProvider> byType,
            Map<String, Map<Long, Instant>> candidates,
            List<AttachedRetention> attached,
            Map<String, Instant> cutoffs) {
        List<DataRef> records = new ArrayList<>();
        Set<DataRef> seen = new HashSet<>();
        Deque<DataRef> queue = new ArrayDeque<>(List.of(root));
        while (!queue.isEmpty()) {
            DataRef record = queue.poll();
            if (!seen.add(record)) {
                continue;
            }
            Map<Long, Instant> expired = candidates.get(record.dataType());
            if (expired == null || !expired.containsKey(record.id())) {
                return Optional.empty();
            }
            records.add(record);
            queue.addAll(byType.get(record.dataType()).referencesTo(record.id()));
        }
        Map<AttachedRetention, List<Attached>> parts = new LinkedHashMap<>();
        for (DataRef record : records) {
            for (AttachedRetention part : attached) {
                List<Attached> found = part.attachedTo(
                        record, byType.get(record.dataType()).retentionKind(), cutoffs.get(part.dataType()));
                if (found.stream().anyMatch(a -> !a.expired())) {
                    return Optional.empty();
                }
                parts.computeIfAbsent(part, p -> new ArrayList<>()).addAll(found);
            }
        }
        return Optional.of(new Chain(records, parts));
    }

    /** Một chuỗi đã hết hạn toàn bộ: bản ghi (gốc trước, bản trỏ tới sau) và phần đi kèm. */
    private record Chain(List<DataRef> records, Map<AttachedRetention, List<Attached>> parts) {

        /** Xóa phần đi kèm trước, rồi bản ghi theo thứ tự ngược (bản trỏ tới trước bản bị trỏ). */
        void delete(Map<String, RetentionProvider> byType) {
            parts.forEach((part, found) -> {
                if (!found.isEmpty()) {
                    part.delete(found.stream().map(Attached::id).toList());
                }
            });
            for (DataRef record : records.reversed()) {
                byType.get(record.dataType()).delete(List.of(record.id()));
            }
        }
    }

    /** Số bản ghi đã xóa và khoảng thời gian của dữ liệu, theo loại (FR-051). */
    private static final class Tally {

        private final Map<String, Range> byType = new LinkedHashMap<>();

        void add(Chain chain, Map<String, Map<Long, Instant>> candidates) {
            for (DataRef record : chain.records()) {
                add(record.dataType(), candidates.get(record.dataType()).get(record.id()));
            }
            chain.parts().forEach((part, found) -> found.forEach(a -> add(part.dataType(), a.at())));
        }

        private void add(String type, Instant at) {
            byType.computeIfAbsent(type, t -> new Range()).add(at);
        }

        int total() {
            return byType.values().stream().mapToInt(r -> r.count).sum();
        }

        Map<String, Object> summary() {
            Map<String, Object> summary = new LinkedHashMap<>();
            byType.forEach((type, range) -> summary.put(
                    type, Map.of("count", range.count, "from", range.from.toString(), "to", range.to.toString())));
            return summary;
        }
    }

    private static final class Range {
        private int count;
        private Instant from;
        private Instant to;

        void add(Instant at) {
            count++;
            from = from == null || at.isBefore(from) ? at : from;
            to = to == null || at.isAfter(to) ? at : to;
        }
    }

    private static String codes(Map<ParameterCode, JsonNode> applied) {
        return applied.isEmpty()
                ? ParameterCode.CFG_M15_04.code()
                : applied.keySet().stream().map(ParameterCode::code).collect(Collectors.joining(", "));
    }

    private static Map<String, Object> byCode(Map<ParameterCode, JsonNode> applied) {
        Map<String, Object> values = new LinkedHashMap<>();
        applied.forEach((code, value) -> values.put(code.code(), value));
        return values;
    }
}
