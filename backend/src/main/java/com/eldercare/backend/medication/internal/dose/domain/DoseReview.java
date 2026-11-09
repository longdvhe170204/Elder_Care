package com.eldercare.backend.medication.internal.dose.domain;

import java.time.Instant;

/**
 * Bản ghi chờ xem lại liều (FR-029): dòng {@code simple_record} loại {@code M07.DOSE_REVIEW} (006 T069, {@code V006_98});
 * {@code DoseReviewRepository} đọc, ghi qua {@code SimpleRecordApi}.
 */
public class DoseReview {

    private Long id;

    private Long doseResultId;

    private Long prnUsageId;

    private Long floorAreaId;

    private String reviewerCandidate;

    private DoseReviewStatus status = DoseReviewStatus.PENDING_REVIEW;

    private Long reviewerId;

    private Instant reviewedAt;

    private String note;

    private int version;

    private Instant createdAt;

    private Instant updatedAt;

    protected DoseReview() {}

    public DoseReview(
            Long doseResultId, Long prnUsageId, Long floorAreaId, String reviewerCandidate, String note, Instant now) {
        this.doseResultId = doseResultId;
        this.prnUsageId = prnUsageId;
        this.floorAreaId = floorAreaId;
        this.reviewerCandidate = reviewerCandidate;
        this.status = DoseReviewStatus.PENDING_REVIEW;
        this.note = note;
        this.createdAt = now;
        this.updatedAt = now;
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

    /** Gán id, phiên bản, thời điểm tạo của dòng bảng chung sau khi đọc hoặc ghi. */
    public void restoreRow(long id, int version, Instant createdAt) {
        this.id = id;
        this.version = version;
        this.createdAt = createdAt;
    }

    /** Dựng lại từ nội dung đã lưu. */
    public static DoseReview restore(DoseReviewStatus status, DoseReviewContent c) {
        DoseReview review = new DoseReview(
                c.doseResultId(), c.prnUsageId(), c.floorAreaId(), c.reviewerCandidate(), c.note(), c.updatedAt());
        review.status = status;
        review.reviewerId = c.reviewerId();
        review.reviewedAt = c.reviewedAt();
        return review;
    }

    public DoseReviewContent content() {
        return new DoseReviewContent(
                doseResultId,
                prnUsageId,
                floorAreaId,
                reviewerCandidate,
                reviewerId,
                reviewedAt,
                note,
                updatedAt,
                null);
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
