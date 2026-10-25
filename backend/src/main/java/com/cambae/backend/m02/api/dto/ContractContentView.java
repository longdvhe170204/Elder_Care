package com.cambae.backend.m02.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import tools.jackson.databind.JsonNode;

/**
 * Nội dung hợp đồng hiệu lực tại một ngày (FR-024): phiên bản nội dung kèm đơn giá đã ghi theo mã dịch vụ.
 * {@code addendumId} rỗng với phiên bản đầu. Trường tài chính thuộc nhóm {@code FINANCE} khi trả ra ngoài.
 */
public record ContractContentView(
        long contractId,
        int version,
        LocalDate effectiveFrom,
        Long addendumId,
        JsonNode content,
        Map<Long, BigDecimal> recordedUnitPrices) {}
