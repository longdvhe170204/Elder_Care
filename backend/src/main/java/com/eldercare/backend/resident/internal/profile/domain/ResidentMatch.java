package com.eldercare.backend.resident.internal.profile.domain;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.LocalDate;

/** Hồ sơ đang mang một CCCD hoặc một phần nhận dạng của hồ sơ trùng. */
public record ResidentMatch(
        long id, String profileCode, String fullName, LocalDate dateOfBirth, ResidentStatus status) {}
