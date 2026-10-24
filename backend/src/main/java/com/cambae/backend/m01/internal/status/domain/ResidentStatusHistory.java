package com.cambae.backend.m01.internal.status.domain;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Lịch sử trạng thái, nhóm 3 chỉ thêm (BR-M01-04, FR-046; data-model mục 3). Hồ sơ mới có bản ghi đầu với lệnh
 * {@code CREATE_PROFILE}, không có trạng thái "từ" và không cần lý do (plan, điểm báo lại 7).
 */
@Entity
@Table(name = "resident_status_history")
@DataGroup(DataGroup.Group.RECORD)
public class ResidentStatusHistory {

    public static final String CREATE_PROFILE = "CREATE_PROFILE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long residentId;

    @Enumerated(EnumType.STRING)
    private ResidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private ResidentStatus toStatus;

    private String command;

    private Instant occurredAt;

    private long performedBy;

    private boolean bySystem;

    private String reason;

    private String basisType;

    private Long basisId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String conditionWaiver;

    protected ResidentStatusHistory() {}

    private ResidentStatusHistory(
            long residentId, ResidentStatus from, ResidentStatus to, String command, Instant at, long performedBy) {
        this.residentId = residentId;
        this.fromStatus = from;
        this.toStatus = to;
        this.command = command;
        this.occurredAt = at;
        this.performedBy = performedBy;
    }

    /** Bản ghi đầu tiên của hồ sơ vừa tạo. */
    public static ResidentStatusHistory profileCreated(long residentId, Instant at, long performedBy) {
        return new ResidentStatusHistory(
                residentId, null, ResidentStatus.ADMISSION_IN_PROGRESS, CREATE_PROFILE, at, performedBy);
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
            String conditionWaiver) {
        ResidentStatusHistory entry = new ResidentStatusHistory(residentId, from, to, command, at, performedBy);
        entry.bySystem = bySystem;
        entry.reason = reason;
        entry.basisType = basisType;
        entry.basisId = basisId;
        entry.conditionWaiver = conditionWaiver;
        return entry;
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

    public String getConditionWaiver() {
        return conditionWaiver;
    }
}
