package com.cambae.backend.m02.internal.contract.domain;

import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

/**
 * Phiên bản nội dung hợp đồng (nhóm 3, chỉ thêm; FR-024): 0 lúc ký, tăng theo phụ lục. {@code content} là chụp nội dung
 * kèm đơn giá đã ghi của từng dịch vụ ({@code recordedUnitPrice}); {@code addendumId} rỗng ở phiên bản 0.
 */
public record ContractVersion(
        long id, long contractId, int sequenceNo, LocalDate effectiveFrom, Long addendumId, JsonNode content) {}
