package com.cambae.backend.m07.internal.domain;

import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Column;
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
 * Dose Review Entity (Group 2 - Business with lifecycle; FR-029, Q-66; data-model section 5).
 * Table {@code dose_review}.
 */
@Entity
@Table(name = "dose_review")
@DataGroup(DataGroup.Group.BUSINESS)
public class DoseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dose_result_id", unique = true)
    private Long doseResultId;

    @Column(name = "prn_usage_id")
    private Long prnUsageId;

    @Column(name = "floor_area_id")
    private Long floorAreaId;

    @Column(name = "reviewer_candidate", columnDefinition = "json")
    private String reviewerCandidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DoseReviewStatus status = DoseReviewStatus.PENDING_REVIEW;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(length = 2000)
    private String note;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DoseReview() {}

    public DoseReview(Long doseResultId, Long prnUsageId, Long floorAreaId, String reviewerCandidate, String note) {
        this.doseResultId = doseResultId;
        this.prnUsageId = prnUsageId;
        this.floorAreaId = floorAreaId;
        this.reviewerCandidate = reviewerCandidate;
        this.status = DoseReviewStatus.PENDING_REVIEW;
        this.note = note;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void accept(Long reviewerId, String reviewNote, Instant now) {
        this.status = DoseReviewStatus.ACCEPTED;
        this.reviewerId = reviewerId;
        this.reviewedAt = now;
        if (reviewNote != null) {
            this.note = reviewNote;
        }
        this.updatedAt = now;
    }

    public void requireCorrection(Long reviewerId, String reviewNote, Instant now) {
        this.status = DoseReviewStatus.REQUIRES_CORRECTION;
        this.reviewerId = reviewerId;
        this.reviewedAt = now;
        if (reviewNote != null) {
            this.note = reviewNote;
        }
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getDoseResultId() {
        return doseResultId;
    }

    public Long getPrnUsageId() {
        return prnUsageId;
    }

    public Long getFloorAreaId() {
        return floorAreaId;
    }

    public String getReviewerCandidate() {
        return reviewerCandidate;
    }

    public DoseReviewStatus getStatus() {
        return status;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getNote() {
        return note;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
