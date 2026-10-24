package com.cambae.backend.m01.internal.health.domain;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.m01.api.dto.HealthItemCheckSource;
import com.cambae.backend.m01.api.dto.HealthItemType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Mục sức khỏe, nhóm 2 nhưng nội dung bất biến (data-model mục 5; DBR-04, BR-M01-07): dị ứng, bệnh nền, tiền sử. Chỉ có
 * hai thay đổi: loại trừ một chiều và gỡ cờ chờ kiểm tra (BR-M01-12); trigger ở cơ sở dữ liệu chặn phần còn lại. Cột sinh
 * {@code not_auto_checked} không ánh xạ, tính lại ở {@link #isNotAutoChecked}.
 */
@Entity
@Table(name = "health_item")
@DataGroup(DataGroup.Group.BUSINESS)
public class HealthItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long residentId;

    @Enumerated(EnumType.STRING)
    private HealthItemType type;

    private Long allergenId;

    private String freeText;

    private Long severityId;

    private long sourceId;

    private long recordedBy;

    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    private HealthItemStatus status = HealthItemStatus.ACTIVE;

    private Instant excludedAt;

    private Long excludedBy;

    private String exclusionReason;

    private boolean pendingMedicationCheck;

    private boolean pendingNutritionCheck;

    @Version
    private int version;

    protected HealthItem() {}

    /** Mục mới Hiệu lực; dị ứng chọn từ danh mục đặt cờ chờ kiểm tra thuốc và chế độ ăn (BR-M01-12). */
    public HealthItem(
            long residentId,
            HealthItemType type,
            Long allergenId,
            String freeText,
            Long severityId,
            long sourceId,
            long recordedBy,
            Instant now) {
        this.residentId = residentId;
        this.type = type;
        this.allergenId = allergenId;
        this.freeText = freeText;
        this.severityId = severityId;
        this.sourceId = sourceId;
        this.recordedBy = recordedBy;
        this.recordedAt = now;
        boolean checkable = type == HealthItemType.ALLERGY && allergenId != null;
        this.pendingMedicationCheck = checkable;
        this.pendingNutritionCheck = checkable;
    }

    public void exclude(long by, Instant now, String reason) {
        this.status = HealthItemStatus.EXCLUDED;
        this.excludedAt = now;
        this.excludedBy = by;
        this.exclusionReason = reason;
    }

    public void markChecked(HealthItemCheckSource source) {
        switch (source) {
            case MEDICATION -> pendingMedicationCheck = false;
            case NUTRITION -> pendingNutritionCheck = false;
        }
    }

    public boolean isNotAutoChecked() {
        return type == HealthItemType.ALLERGY && allergenId == null;
    }

    public boolean isPendingCheck() {
        return pendingMedicationCheck || pendingNutritionCheck;
    }

    public boolean isActive() {
        return status == HealthItemStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public long getResidentId() {
        return residentId;
    }

    public HealthItemType getType() {
        return type;
    }

    public Long getAllergenId() {
        return allergenId;
    }

    public String getFreeText() {
        return freeText;
    }

    public Long getSeverityId() {
        return severityId;
    }

    public long getSourceId() {
        return sourceId;
    }

    public long getRecordedBy() {
        return recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public HealthItemStatus getStatus() {
        return status;
    }

    public Instant getExcludedAt() {
        return excludedAt;
    }

    public Long getExcludedBy() {
        return excludedBy;
    }

    public String getExclusionReason() {
        return exclusionReason;
    }

    public int getVersion() {
        return version;
    }
}
