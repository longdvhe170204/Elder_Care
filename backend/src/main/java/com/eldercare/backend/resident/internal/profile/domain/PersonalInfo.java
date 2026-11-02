package com.eldercare.backend.resident.internal.profile.domain;

import com.eldercare.backend.resident.api.dto.Gender;
import java.time.LocalDate;

/**
 * Thông tin cá nhân của hồ sơ (FR-001, FR-006): chỉ đổi qua lệnh Cập nhật thông tin cá nhân. Họ tên, ngày sinh, giới
 * tính bắt buộc; loại và số giấy tờ đi cùng nhau.
 */
public record PersonalInfo(
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        IdDocumentType idDocumentType,
        String idDocumentNumber,
        Long photoFileId,
        String permanentAddress,
        String contactAddress,
        Contact contact,
        String specialNotes) {}
