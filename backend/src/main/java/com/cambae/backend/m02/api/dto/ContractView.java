package com.cambae.backend.m02.api.dto;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import java.time.LocalDate;

/**
 * Hợp đồng kèm trạng thái và dấu quá hạn (HopDongDocApi.hopDong). {@code currentEndDate} là ngày kết thúc sau gia hạn;
 * {@code overdueSince} rỗng khi chưa quá hạn (FR-029). Provisional: danh sách trường chờ 003, 005, 010, 012, 017, 018
 * xác nhận khi dùng.
 */
public record ContractView(
        long id,
        String contractNumber,
        long residentId,
        ResidenceType residenceType,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate currentEndDate,
        CareLevel careLevel,
        ContractStatus status,
        LocalDate overdueSince) {}
