package com.cambae.backend.m02.api.dto;

import com.cambae.backend.m01.api.dto.ResidenceType;

/**
 * Loại lưu trú của một người tại ngày (016 FR-022): hợp đồng hiệu lực kể cả quá hạn, hoặc chờ hiệu lực, hoặc chưa xác
 * định khi {@code residenceType} rỗng.
 */
public record ResidenceTypeAtDateView(long residentId, ResidenceType residenceType, ContractStatus contractStatus) {}
