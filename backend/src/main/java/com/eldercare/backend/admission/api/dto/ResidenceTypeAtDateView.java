package com.eldercare.backend.admission.api.dto;

import com.eldercare.backend.resident.api.dto.ResidenceType;

/**
 * Loại lưu trú của một người tại ngày (016 FR-022): hợp đồng hiệu lực kể cả quá hạn, hoặc chờ hiệu lực, hoặc chưa xác
 * định khi {@code residenceType} rỗng.
 */
public record ResidenceTypeAtDateView(long residentId, ResidenceType residenceType, ContractStatus contractStatus) {}
