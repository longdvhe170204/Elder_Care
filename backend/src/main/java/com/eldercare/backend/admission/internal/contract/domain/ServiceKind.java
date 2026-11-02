package com.eldercare.backend.admission.internal.contract.domain;

import com.eldercare.backend.resident.api.dto.CareLevel;

/** Loại, mức chăm sóc (chỉ dịch vụ chăm sóc, FR-025, Q-297) và trạng thái Hiệu lực của dịch vụ. */
public record ServiceKind(String type, CareLevel careLevel, boolean active) {}
