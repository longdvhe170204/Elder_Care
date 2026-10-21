package com.cambae.backend.m01.internal.status.domain;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

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

    public Long getId() {
        return id;
    }
}
