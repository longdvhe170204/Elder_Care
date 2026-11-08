package com.eldercare.backend.staffing.internal.swap.service;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDay;
import com.eldercare.backend.staffing.internal.swap.domain.LeaveDetail;
import com.eldercare.backend.staffing.internal.swap.domain.OpenShiftLock;
import com.eldercare.backend.staffing.internal.swap.domain.SwapDetail;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Bản ghi của 015 ở {@code simple_record} (data-model mục 0.1, R-15): chi tiết đổi ca, chi tiết nghỉ, khóa "một yêu cầu
 * mở mỗi (ca, nhân viên)", nghỉ theo ngày. Không tự mở transaction; chạy trong transaction của lệnh.
 */
@Component
class SwapRecords {

    static final String LOCK_OPEN = "OPEN";
    static final String LOCK_RELEASED = "RELEASED";
    private static final String REQUEST = "M15.APPROVAL_REQUEST";

    private final SimpleRecordApi records;
    private final com.eldercare.backend.common.time.BusinessTime time;

    SwapRecords(SimpleRecordApi records, com.eldercare.backend.common.time.BusinessTime time) {
        this.records = records;
        this.time = time;
    }

    // ---- Chi tiết đổi ca -------------------------------------------------------------------------------------------

    SimpleRecord<SwapDetail> createSwap(SwapDetail detail, String status, long createdBy) {
        return records.create(
                SwapDetail.KIND,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef(REQUEST, detail.requestId()),
                        status,
                        detail,
                        null,
                        null,
                        SwapDetail.uniqueKey(detail.requestId()),
                        createdBy,
                        false));
    }

    SimpleRecord<SwapDetail> swap(long requestId) {
        return records.findByUniqueKey(SwapDetail.KIND, SwapDetail.uniqueKey(requestId))
                .orElseThrow(NotFoundException::new);
    }

    Optional<SimpleRecord<SwapDetail>> findSwap(long requestId) {
        return records.findByUniqueKey(SwapDetail.KIND, SwapDetail.uniqueKey(requestId));
    }

    SimpleRecord<SwapDetail> lockSwap(long requestId) {
        return records.lockForUpdate(SwapDetail.KIND, swap(requestId).id());
    }

    SimpleRecord<SwapDetail> updateSwap(SimpleRecord<SwapDetail> row, String status, SwapDetail detail, boolean last) {
        return records.change(
                        SwapDetail.KIND,
                        row.id(),
                        row.version(),
                        new RecordChange<>(status, last ? detail.closed(time.now()) : detail, null, row.uniqueKey(), last))
                .after();
    }

    List<SimpleRecord<SwapDetail>> swaps(Set<String> statuses) {
        return records.listByStatus(SwapDetail.KIND, statuses);
    }

    // ---- Chi tiết nghỉ -------------------------------------------------------------------------------------------

    SimpleRecord<LeaveDetail> createLeave(LeaveDetail detail, String status, long createdBy) {
        return records.create(
                LeaveDetail.KIND,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef(REQUEST, detail.requestId()),
                        status,
                        detail,
                        null,
                        null,
                        LeaveDetail.uniqueKey(detail.requestId()),
                        createdBy,
                        false));
    }

    SimpleRecord<LeaveDetail> leave(long requestId) {
        return records.findByUniqueKey(LeaveDetail.KIND, LeaveDetail.uniqueKey(requestId))
                .orElseThrow(NotFoundException::new);
    }

    Optional<SimpleRecord<LeaveDetail>> findLeave(long requestId) {
        return records.findByUniqueKey(LeaveDetail.KIND, LeaveDetail.uniqueKey(requestId));
    }

    SimpleRecord<LeaveDetail> lockLeave(long requestId) {
        return records.lockForUpdate(LeaveDetail.KIND, leave(requestId).id());
    }

    SimpleRecord<LeaveDetail> updateLeave(
            SimpleRecord<LeaveDetail> row, String status, LeaveDetail detail, boolean last) {
        return records.change(
                        LeaveDetail.KIND,
                        row.id(),
                        row.version(),
                        new RecordChange<>(status, last ? detail.closed(time.now()) : detail, null, row.uniqueKey(), last))
                .after();
    }

    /** Đánh dấu đã nhắc một lần; false khi đã nhắc hoặc yêu cầu không còn chờ. */
    boolean markLeaveReminded(long requestId, java.time.Instant at) {
        SimpleRecord<LeaveDetail> row = lockLeave(requestId);
        if (row.content().remindedAt() != null || !"PENDING_APPROVAL".equals(row.status())) {
            return false;
        }
        updateLeave(row, row.status(), row.content().reminded(at), false);
        return true;
    }

    List<SimpleRecord<LeaveDetail>> leaves(Set<String> statuses) {
        return records.listByStatus(LeaveDetail.KIND, statuses);
    }

    // ---- Khóa (ca, nhân viên) -----------------------------------------------------------------------------------

    void acquireDay(long staffId, LocalDate date, long requestId, long by) {
        acquireKey(OpenShiftLock.dayKey(staffId, date), requestId, by);
    }

    void releaseDay(long staffId, LocalDate date) {
        releaseKey(OpenShiftLock.dayKey(staffId, date));
    }

    /** FR-019, FR-026: ca nằm trong yêu cầu đổi ca hoặc nghỉ khác đang mở thì 409. */
    void acquire(long shiftId, long staffId, long requestId, long by) {
        acquireKey(OpenShiftLock.uniqueKey(shiftId, staffId), requestId, by);
    }

    private void acquireKey(String key, long requestId, long by) {
        // Khóa đã nhả nhường khóa duy nhất (uniqueKey null), nên còn dòng theo khóa là còn yêu cầu mở.
        if (records.findByUniqueKey(OpenShiftLock.KIND, key).isPresent()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "SHIFT_IN_ANOTHER_REQUEST",
                    "Ca đang nằm trong yêu cầu đổi ca hoặc nghỉ đột xuất khác");
        }
        records.create(
                OpenShiftLock.KIND,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef(REQUEST, requestId),
                        LOCK_OPEN,
                        null,
                        null,
                        null,
                        key,
                        by,
                        false));
    }

    boolean hasOpenLock(long shiftId, long staffId) {
        return records.findByUniqueKey(OpenShiftLock.KIND, OpenShiftLock.uniqueKey(shiftId, staffId))
                .isPresent();
    }

    /** Nhả khóa khi yêu cầu vào trạng thái cuối hoặc mục rời yêu cầu; không có khóa thì bỏ qua. */
    void release(long shiftId, long staffId) {
        releaseKey(OpenShiftLock.uniqueKey(shiftId, staffId));
    }

    private void releaseKey(String key) {
        records.findByUniqueKey(OpenShiftLock.KIND, key)
                .filter(row -> LOCK_OPEN.equals(row.status()))
                .ifPresent(row -> records.change(
                        OpenShiftLock.KIND,
                        row.id(),
                        row.version(),
                        new RecordChange<>(LOCK_RELEASED, null, null, null, true)));
    }

    // ---- Nghỉ theo ngày ----------------------------------------------------------------------------------------

    void recordLeaveDay(LeaveDay day, long by) {
        records.create(
                LeaveDay.KIND,
                new NewSimpleRecord<>(
                        null,
                        new EntityRef(REQUEST, day.requestId()),
                        "APPROVED",
                        day,
                        day.date(),
                        day.date(),
                        LeaveDay.uniqueKey(day.staffId(), day.date()),
                        by,
                        true));
    }

    boolean hasLeaveDay(long staffId, LocalDate date) {
        return records.findByUniqueKey(LeaveDay.KIND, LeaveDay.uniqueKey(staffId, date))
                .isPresent();
    }
}
