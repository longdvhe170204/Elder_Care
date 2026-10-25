package com.cambae.backend.m01.internal.health.domain;

import com.cambae.backend.common.data.DataGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * Hồ sơ sức khỏe ban đầu, nhóm 3 có bản nháp (data-model mục 5; FR-017): sửa được tới khi xác nhận, sau đó trigger ở cơ
 * sở dữ liệu chặn mọi {@code UPDATE} và chỉ đính chính qua 000. Mỗi người cao tuổi tối đa một hồ sơ.
 */
@Entity
@Table(name = "initial_health_record")
@DataGroup(DataGroup.Group.RECORD)
public class InitialHealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long residentId;

    private String bloodType;

    private String mobility;

    private String selfCareAbility;

    private String cognition;

    private String initialNeedsNote;

    private String nutritionNeeds;

    private String currentMedications;

    private String rehabilitationNeeds;

    private long performedBy;

    private Instant performedAt;

    private Long confirmedBy;

    private Instant confirmedAt;

    @Version
    private int version;

    protected InitialHealthRecord() {}

    public InitialHealthRecord(long residentId, InitialHealthRecordContent content, long performedBy, Instant now) {
        this.residentId = residentId;
        this.performedBy = performedBy;
        this.performedAt = now;
        apply(content);
    }

    /** Sửa bản nháp; đã xác nhận thì không sửa được (kiểm ở dịch vụ, trigger chặn thêm một lớp). */
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
