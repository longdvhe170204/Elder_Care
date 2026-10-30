package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.internal.profile.domain.Contact;
import com.eldercare.backend.resident.internal.profile.domain.IdDocumentType;
import com.eldercare.backend.resident.internal.profile.domain.PersonalInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Thân yêu cầu tạo hồ sơ: thông tin cá nhân cùng {@code confirmNotDuplicate} (contracts/api-001.yaml, {@code POST /residents}). */
public record CreateResidentBody(
        @NotNull @Size(max = 200) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String fullName,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dateOfBirth,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Gender gender,

        IdDocumentType idDocumentType,
        String idDocumentNumber,
        Long photoFileId,
        String permanentAddress,
        String contactAddress,
        Contact contact,
        String specialNotes,
        @Schema(defaultValue = "false") Boolean confirmNotDuplicate) {

    public PersonalInfo personalInfo() {
        return new PersonalInfo(
                fullName,
                dateOfBirth,
                gender,
                idDocumentType,
                idDocumentNumber,
                photoFileId,
                permanentAddress,
                contactAddress,
                contact,
                specialNotes);
    }

    public boolean confirmed() {
        return Boolean.TRUE.equals(confirmNotDuplicate);
    }
}
