package com.cambae.backend.m15.correction.internal.domain;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CreationMethod;
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

/** Bản đính chính, nhóm 3, chỉ thêm (data-model mục 4; DBR-23). */
@Entity
@Table(name = "correction")
@DataGroup(DataGroup.Group.RECORD)
public class Correction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recordType;

    private long originalRecordId;

    @Enumerated(EnumType.STRING)
    private CorrectionType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String correctedContent;

    private String reason;

    private long actorId;

    @Enumerated(EnumType.STRING)
    private CreationMethod creationMethod;

    private Long approvalRequestId;

    private Instant recordedAt;

    private int sequenceNo;

    protected Correction() {}

    public Correction(
            String recordType,
            long originalRecordId,
            CorrectionType type,
            String correctedContent,
            String reason,
            long actorId,
            CreationMethod creationMethod,
            Long approvalRequestId,
            Instant recordedAt,
            int sequenceNo) {
        this.recordType = recordType;
        this.originalRecordId = originalRecordId;
        this.type = type;
        this.correctedContent = correctedContent;
        this.reason = reason;
        this.actorId = actorId;
        this.creationMethod = creationMethod;
        this.approvalRequestId = approvalRequestId;
        this.recordedAt = recordedAt;
        this.sequenceNo = sequenceNo;
    }

    public Long getId() {
        return id;
    }

    public String getRecordType() {
        return recordType;
    }

    public long getOriginalRecordId() {
        return originalRecordId;
    }

    public CorrectionType getType() {
        return type;
    }

    public String getCorrectedContent() {
        return correctedContent;
    }

    public String getReason() {
        return reason;
    }

    public long getActorId() {
        return actorId;
    }

    public CreationMethod getCreationMethod() {
        return creationMethod;
    }

    public Long getApprovalRequestId() {
        return approvalRequestId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }
}
