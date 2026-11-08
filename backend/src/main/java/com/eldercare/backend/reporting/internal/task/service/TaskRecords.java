package com.eldercare.backend.reporting.internal.task.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.care.api.TaskStatisticsApi;
import com.eldercare.backend.care.api.dto.TaskReportView;
import com.eldercare.backend.care.api.dto.TaskStatus;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Công việc chăm sóc cho {@code TASK.*} (FR-020, FR-030 → FR-032) đọc {@code TaskStatisticsApi.inRange} của 005: trạng
 * thái 8.3, người đóng ("Hệ thống" là không có người đóng), quy tắc đóng (005 FR-028), nhãn ghi nhận muộn (BR-M04-12) và
 * các mốc vào Quá hạn do 005 xác định. Công việc chung của tầng là công việc không gắn người cao tuổi.
 */
@Component
@Transactional(readOnly = true)
public class TaskRecords {

    static final String COMMON = "COMMON";
    static final String UNASSIGNED = "UNASSIGNED";

    private static final Set<TaskStatus> SYSTEM_CLOSABLE = Set.of(TaskStatus.NOT_PERFORMED, TaskStatus.CANCELLED);

    /** Một công việc; mốc xếp khoảng là thời điểm dự kiến. */
    public record TaskRecord(TaskReportView view) implements MetricRecord {

        @Override
        public String sourceKey() {
            return "M04.TASK:" + view.id();
        }

        @Override
        public Instant referenceTime() {
            return view.plannedAt();
        }

        @Override
        public Long floorAreaId() {
            return view.floorAreaId();
        }

        @Override
        public Long residentId() {
            return view.residentId().orElse(null);
        }

        /** Đóng bởi "Hệ thống" (005 FR-028): Không thực hiện, Hủy mà không có người đóng. */
        boolean closedBySystem() {
            return SYSTEM_CLOSABLE.contains(view.status()) && view.closedById().isEmpty();
        }

        /** Trạng thái; việc do hệ thống đóng tách theo quy tắc đóng (FR-030). */
        String statusKey() {
            return closedBySystem()
                    ? view.status().name() + ":SYSTEM:" + view.closingRule().orElse("UNKNOWN")
                    : view.status().name();
        }

        /** Nhân viên thực hiện; việc chung của tầng một dòng riêng (FR-030). */
        String staffKey() {
            if (view.residentId().isEmpty()) {
                return COMMON;
            }
            return view.performerId().map(String::valueOf).orElse(UNASSIGNED);
        }

        boolean overdueWithin(Instant from, Instant to) {
            return view.overdueAt().stream().anyMatch(t -> !t.isBefore(from) && t.isBefore(to));
        }
    }

    private final ObjectProvider<TaskStatisticsApi> statistics;

    public TaskRecords(ObjectProvider<TaskStatisticsApi> statistics) {
        this.statistics = statistics;
    }

    public Stream<TaskRecord> in(ScopeFilter rough, Instant from, Instant to) {
        TaskStatisticsApi api = statistics.getIfAvailable();
        if (api == null) {
            return Stream.empty();
        }
        return api.inRange(rough, from, to).map(TaskRecord::new);
    }

    static DetailRow row(TaskRecord r) {
        TaskReportView v = r.view();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskTypeId", v.taskTypeId());
        result.put("importance", v.importance().name());
        result.put("shiftId", v.shiftId());
        result.put("closingRule", v.closingRule().orElse(null));
        result.put("closedBySystem", r.closedBySystem());
        result.put("performedAt", v.performedAt().orElse(null));
        result.put("recordedLate", v.recordedLate());
        return new DetailRow(
                r.sourceKey(),
                "005",
                "CARE_TASK",
                v.status().name(),
                r.referenceTime(),
                r.floorAreaId(),
                null,
                r.residentId(),
                null,
                v.performerId().map(String::valueOf).orElse(null),
                result,
                null,
                Map.of(),
                false,
                false,
                false);
    }
}
