package com.eldercare.backend.resident.internal.status.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.Map;

/**
 * Lịch sử trạng thái, nhóm 3 chỉ thêm (BR-M01-04, FR-046; data-model mục 3, 0.1). Từ 001 T084 là dòng {@code POINT} của
 * {@code status_history} ({@link #TRACK}): giá trị trước, giá trị là trạng thái "từ", "đến"; lệnh, người thực hiện, lý
 * do, căn cứ ở cột chung; miễn điều kiện ở {@code details}. Hồ sơ mới có bản ghi đầu với lệnh {@code CREATE_PROFILE},
 * không có trạng thái "từ" và không cần lý do (plan, điểm báo lại 7); lý do bắt buộc với lệnh khác không do hệ thống
 * ({@code reasonRequired}, A1 2026-11-04).
 */
public class ResidentStatusHistory {

    public static final String CREATE_PROFILE = "CREATE_PROFILE";

    /** Thuộc tính {@code STATUS} của đối tượng {@code M01.RESIDENT}. */
    public static final HistoryTrack<StatusChangeDetails> TRACK =
            new HistoryTrack<>("M01.RESIDENT", "STATUS", StatusChangeDetails.class, true);

    /**
     * Đính chính thời điểm thực tế của một dòng {@link #TRACK} (014 FR-031a (a); đổi api chủ dự án duyệt 2026-11-11). Lịch
     * sử chỉ thêm (DBR-23) nên không sửa {@code starts_at} của dòng gốc: mỗi lần đính chính là một dòng {@code POINT} mới,
     * {@code value_id} là mã dòng gốc, {@code starts_at} là thời điểm đúng; lần mới nhất thắng khi đọc.
     */
    public static final HistoryTrack<Void> TIME_TRACK = new HistoryTrack<>("M01.RESIDENT", "STATUS_TIME", Void.class);

    public static final String CORRECT_TIME = "CORRECT_TIME";

    private final Long id;
    private final long residentId;
    private final ResidentStatus fromStatus;
    private final ResidentStatus toStatus;
    private final String command;
    private final Instant occurredAt;
    private final long performedBy;
    private final boolean bySystem;
    private final String reason;
    private final String basisType;
    private final Long basisId;
    private final Map<String, Object> conditionWaiver;

    /** Dòng đã lưu (đọc từ {@code status_history}) hoặc chưa lưu ({@code id} {@code null}). */
    public ResidentStatusHistory(
            Long id,
            long residentId,
            ResidentStatus fromStatus,
            ResidentStatus toStatus,
            String command,
            Instant occurredAt,
            long performedBy,
            boolean bySystem,
            String reason,
            String basisType,
            Long basisId,
            Map<String, Object> conditionWaiver) {
        this.id = id;
        this.residentId = residentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.command = command;
        this.occurredAt = occurredAt;
        this.performedBy = performedBy;
        this.bySystem = bySystem;
        this.reason = reason;
        this.basisType = basisType;
        this.basisId = basisId;
        this.conditionWaiver = conditionWaiver;
    }

    /** Bản ghi đầu tiên của hồ sơ vừa tạo. */
    public static ResidentStatusHistory profileCreated(long residentId, Instant at, long performedBy) {
        return new ResidentStatusHistory(
                null,
                residentId,
                null,
                ResidentStatus.ADMISSION_IN_PROGRESS,
                CREATE_PROFILE,
                at,
                performedBy,
                false,
                null,
                null,
                null,
                null);
    }

    /** Một lần chuyển trạng thái (FR-046); {@code reason} rỗng chỉ với lệnh do hệ thống. */
    public static ResidentStatusHistory transition(
            long residentId,
            ResidentStatus from,
            ResidentStatus to,
            String command,
            Instant at,
            long performedBy,
            boolean bySystem,
            String reason,
            String basisType,
            Long basisId,
            Map<String, Object> conditionWaiver) {
        return new ResidentStatusHistory(
                null,
                residentId,
                from,
                to,
                command,
                at,
                performedBy,
                bySystem,
                reason,
                basisType,
                basisId,
                conditionWaiver);
    }

    /** Cùng dòng, mang id đã cấp khi lưu. */
    public ResidentStatusHistory withId(long savedId) {
        return new ResidentStatusHistory(
                savedId,
                residentId,
                fromStatus,
                toStatus,
                command,
                occurredAt,
                performedBy,
                bySystem,
                reason,
                basisType,
                basisId,
                conditionWaiver);
    }

    /** Cùng dòng, thời điểm đã đính chính ({@link #TIME_TRACK}). */
    public ResidentStatusHistory withOccurredAt(Instant corrected) {
        return new ResidentStatusHistory(
                id,
                residentId,
                fromStatus,
                toStatus,
                command,
                corrected,
                performedBy,
                bySystem,
                reason,
                basisType,
                basisId,
                conditionWaiver);
    }

    public Long getId() {
        return id;
    }

    public long getResidentId() {
        return residentId;
    }

    public ResidentStatus getFromStatus() {
        return fromStatus;
    }

    public ResidentStatus getToStatus() {
        return toStatus;
    }

    public String getCommand() {
        return command;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public long getPerformedBy() {
        return performedBy;
    }

    public boolean isBySystem() {
        return bySystem;
    }

    public String getReason() {
        return reason;
    }

    public String getBasisType() {
        return basisType;
    }

    public Long getBasisId() {
        return basisId;
    }

    public Map<String, Object> getConditionWaiver() {
        return conditionWaiver;
    }
}
