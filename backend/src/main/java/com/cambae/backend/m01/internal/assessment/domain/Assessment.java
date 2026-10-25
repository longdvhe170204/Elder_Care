package com.cambae.backend.m01.internal.assessment.domain;

import com.cambae.backend.common.data.DataGroup;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Lần đánh giá, nhóm 3 có bản nháp (data-model mục 7; FR-025, FR-026): sửa được khi còn Nháp; sau xác nhận hoặc hủy trigger
 * ở cơ sở dữ liệu chặn mọi {@code UPDATE}, chỉ đính chính qua 000. Không có điểm hay mức đề xuất (FR-024).
 */
@Entity
@Table(name = "assessment")
@DataGroup(DataGroup.Group.RECORD)
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long residentId;

    @Enumerated(EnumType.STRING)
    private AssessmentType type;

    private String basis;

    private long performedBy;

    private Instant performedAt;

    private String comment;

    private String careNote;

    @Enumerated(EnumType.STRING)
    private CareLevel careLevel;

    @Enumerated(EnumType.STRING)
    private ResidenceType proposedResidenceType;

    @Enumerated(EnumType.STRING)
    private AssessmentStatus status = AssessmentStatus.DRAFT;

    private Long confirmedBy;

    private Instant confirmedAt;

    private Long discardedBy;

    private Instant discardedAt;

    private String discardReason;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "assessment_care_need", joinColumns = @JoinColumn(name = "assessment_id"))
    @Column(name = "care_need_id")
    private Set<Long> careNeedIds = new HashSet<>();

    @Version
    private int version;

    protected Assessment() {}

    public Assessment(long residentId, AssessmentType type, String basis, long performedBy, Instant now) {
        this.residentId = residentId;
        this.type = type;
        this.basis = basis;
        this.performedBy = performedBy;
        this.performedAt = now;
    }

    /** Sửa bản nháp; kiểm trạng thái ở dịch vụ, trigger chặn thêm một lớp. */
    public void editDraft(
            String basis,
            String comment,
            String careNote,
            CareLevel careLevel,
            Set<Long> careNeedIds,
            ResidenceType proposedResidenceType) {
        this.basis = basis;
        this.comment = comment;
        this.careNote = careNote;
        this.careLevel = careLevel;
        this.proposedResidenceType = proposedResidenceType;
        this.careNeedIds.retainAll(careNeedIds);
        this.careNeedIds.addAll(careNeedIds);
    }

    public void confirm(long by, Instant now) {
        this.status = AssessmentStatus.CONFIRMED;
        this.confirmedBy = by;
        this.confirmedAt = now;
    }

    public void discard(long by, Instant now, String reason) {
        this.status = AssessmentStatus.DISCARDED;
        this.discardedBy = by;
        this.discardedAt = now;
        this.discardReason = reason;
    }

    public boolean isDraft() {
        return status == AssessmentStatus.DRAFT;
    }

    public boolean isConfirmed() {
        return status == AssessmentStatus.CONFIRMED;
    }

    public Long getId() {
        return id;
    }

    public long getResidentId() {
        return residentId;
    }

    public AssessmentType getType() {
        return type;
    }

    public String getBasis() {
        return basis;
    }

    public long getPerformedBy() {
        return performedBy;
    }

    public String getComment() {
        return comment;
    }

    public String getCareNote() {
        return careNote;
    }

    public CareLevel getCareLevel() {
        return careLevel;
    }

    public ResidenceType getProposedResidenceType() {
        return proposedResidenceType;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public Long getConfirmedBy() {
        return confirmedBy;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Set<Long> getCareNeedIds() {
        return Set.copyOf(careNeedIds);
    }

    public int getVersion() {
        return version;
    }
}
