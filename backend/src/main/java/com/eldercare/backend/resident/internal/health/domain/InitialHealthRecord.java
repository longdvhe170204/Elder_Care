package com.eldercare.backend.resident.internal.health.domain;

import java.time.Instant;

/**
 * Hồ sơ sức khỏe ban đầu, nhóm 3 có bản nháp (data-model mục 5, 0.1; FR-017): sửa được tới khi xác nhận, sau đó dòng
 * {@code simple_record} bị khóa ở cơ sở dữ liệu và chỉ đính chính qua 000. Mỗi người cao tuổi tối đa một hồ sơ.
 */
public class InitialHealthRecord {

    private final Long id;
    private final long residentId;
    private String bloodType;
    private String mobility;
    private String selfCareAbility;
    private String cognition;
    private String initialNeedsNote;
    private String nutritionNeeds;
    private String currentMedications;
    private String rehabilitationNeeds;
    private final long performedBy;
    private final Instant performedAt;
    private Long confirmedBy;
    private Instant confirmedAt;
    private final int version;

    public InitialHealthRecord(long residentId, InitialHealthRecordContent content, long performedBy, Instant now) {
        this(null, residentId, content, performedBy, now, null, null, 0);
    }

    /** Hồ sơ đã lưu; {@code confirmedAt} là lúc dòng bị khóa. */
    public InitialHealthRecord(
            Long id,
            long residentId,
            InitialHealthRecordContent content,
            long performedBy,
            Instant performedAt,
            Long confirmedBy,
            Instant confirmedAt,
            int version) {
        this.id = id;
        this.residentId = residentId;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
        this.version = version;
        apply(content);
    }

    /** Sửa bản nháp; đã xác nhận thì không sửa được (kiểm ở dịch vụ, khóa dòng chặn thêm một lớp). */
    public void editDraft(InitialHealthRecordContent content) {
        apply(content);
    }

    public void confirm(long by, Instant now) {
        this.confirmedBy = by;
        this.confirmedAt = now;
    }

    public boolean isConfirmed() {
        return confirmedAt != null;
    }

    public InitialHealthRecordContent content() {
        return new InitialHealthRecordContent(
                bloodType,
                mobility,
                selfCareAbility,
                cognition,
                initialNeedsNote,
                nutritionNeeds,
                currentMedications,
                rehabilitationNeeds);
    }

    /** Nội dung lưu ở cột {@code content}. */
    public InitialHealthRecordData data() {
        return new InitialHealthRecordData(
                bloodType,
                mobility,
                selfCareAbility,
                cognition,
                initialNeedsNote,
                nutritionNeeds,
                currentMedications,
                rehabilitationNeeds,
                performedBy,
                performedAt,
                confirmedBy);
    }

    private void apply(InitialHealthRecordContent content) {
        this.bloodType = content.bloodType();
        this.mobility = content.mobility();
        this.selfCareAbility = content.selfCareAbility();
        this.cognition = content.cognition();
        this.initialNeedsNote = content.initialNeedsNote();
        this.nutritionNeeds = content.nutritionNeeds();
        this.currentMedications = content.currentMedications();
        this.rehabilitationNeeds = content.rehabilitationNeeds();
    }

    public Long getId() {
        return id;
    }

    public long getResidentId() {
        return residentId;
    }

    public long getPerformedBy() {
        return performedBy;
    }

    public Long getConfirmedBy() {
        return confirmedBy;
    }

    public int getVersion() {
        return version;
    }
}
