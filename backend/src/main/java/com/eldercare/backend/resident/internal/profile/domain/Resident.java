package com.eldercare.backend.resident.internal.profile.domain;

import com.eldercare.backend.common.data.DataGroup;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
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
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Hồ sơ người cao tuổi, nhóm 2 (data-model mục 2; FR-001 → FR-006). Không có setter trạng thái: trạng thái chỉ đặt lúc
 * tạo, các lần chuyển sau thuộc 004 qua lệnh trạng thái (BR-M01-04). Thông tin cá nhân chỉ đổi qua
 * {@link #changePersonalInfo}. Hai cột sinh của bảng ({@code full_name_match}, {@code open_citizen_id}) không ánh xạ:
 * cơ sở dữ liệu tự tính.
 */
@Entity
@Table(name = "resident")
@DataGroup(DataGroup.Group.BUSINESS)
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String profileCode;

    private Long previousProfileId;

    private String fullName;

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private IdDocumentType idDocumentType;

    private String idDocumentNumber;

    private Long photoFileId;

    private String permanentAddress;

    private String contactAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private String contact;

    private String specialNotes;

    @Enumerated(EnumType.STRING)
    private ResidentStatus status;

    private Instant statusSince;

    private String currentCareLevel;

    @Version
    private int version;

    private Instant createdAt;

    private long createdBy;

    protected Resident() {}

    /** Hồ sơ mới ở Đang tiếp nhận (FR-002); mã hồ sơ đặt sau khi có id bằng {@link #assignProfileCode}. */
    public Resident(PersonalInfo info, String contactJson, Long previousProfileId, Instant now, long createdBy) {
        this.previousProfileId = previousProfileId;
        this.status = ResidentStatus.ADMISSION_IN_PROGRESS;
        this.statusSince = now;
        this.createdAt = now;
        this.createdBy = createdBy;
        apply(info, contactJson);
    }

    /** {@code NCT-<năm tạo>-<id 6 chữ số>} (FR-003; research H-04). */
    public void assignProfileCode(int year) {
        this.profileCode = String.format("NCT-%d-%06d", year, id);
    }

    /** Chuyển trạng thái; chỉ {@code ResidentStatusService} gọi (FR-044). */
    public void changeStatus(ResidentStatus to, Instant at) {
        this.status = to;
        this.statusSince = at;
    }

    public void changePersonalInfo(PersonalInfo info, String contactJson) {
        apply(info, contactJson);
    }

    private void apply(PersonalInfo info, String contactJson) {
        this.fullName = info.fullName();
        this.dateOfBirth = info.dateOfBirth();
        this.gender = info.gender();
        this.idDocumentType = info.idDocumentType();
        this.idDocumentNumber = info.idDocumentNumber();
        this.photoFileId = info.photoFileId();
        this.permanentAddress = info.permanentAddress();
        this.contactAddress = info.contactAddress();
        this.contact = contactJson;
        this.specialNotes = info.specialNotes();
    }

    public Long getId() {
        return id;
    }

    public String getProfileCode() {
        return profileCode;
    }

    public Long getPreviousProfileId() {
        return previousProfileId;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public IdDocumentType getIdDocumentType() {
        return idDocumentType;
    }

    public String getIdDocumentNumber() {
        return idDocumentNumber;
    }

    public Long getPhotoFileId() {
        return photoFileId;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public String getContact() {
        return contact;
    }

    public String getSpecialNotes() {
        return specialNotes;
    }

    public ResidentStatus getStatus() {
        return status;
    }

    public Instant getStatusSince() {
        return statusSince;
    }

    public String getCurrentCareLevel() {
        return currentCareLevel;
    }

    public int getVersion() {
        return version;
    }
}
