package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.internal.profile.domain.Contact;
import com.cambae.backend.m01.internal.profile.domain.IdDocumentType;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Thông tin cá nhân trong phản hồi: giấy tờ và ảnh thuộc nhóm {@code IDENTITY}, địa chỉ và liên hệ thuộc
 * {@code CONTACT}; 002 bỏ trường người xem không được xem khi tuần tự hóa (FR-007, FR-049). {@code target} chỉ để xét
 * phạm vi, không ra ngoài.
 */
@Schema(name = "PersonalInfo")
public record PersonalInfoBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String fullName,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate dateOfBirth,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Gender gender,
        @FieldGroup(InfoGroup.IDENTITY) IdDocumentType idDocumentType,
        @FieldGroup(InfoGroup.IDENTITY) String idDocumentNumber,
        @FieldGroup(InfoGroup.IDENTITY) Long photoFileId,
        @FieldGroup(InfoGroup.CONTACT) String permanentAddress,
        @FieldGroup(InfoGroup.CONTACT) String contactAddress,
        @FieldGroup(InfoGroup.CONTACT) Contact contact,
        String specialNotes,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    public static PersonalInfoBody of(PersonalInfo info, long residentId) {
        return new PersonalInfoBody(
                info.fullName(),
                info.dateOfBirth(),
                info.gender(),
                info.idDocumentType(),
                info.idDocumentNumber(),
                info.photoFileId(),
                info.permanentAddress(),
                info.contactAddress(),
                info.contact(),
                info.specialNotes(),
                ResidentProfileService.target(residentId));
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
