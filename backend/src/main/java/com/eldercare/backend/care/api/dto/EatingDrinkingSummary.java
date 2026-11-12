package com.eldercare.backend.care.api.dto;

import java.math.BigDecimal;
import java.util.*;

/**
 * T003, T039: tổng hợp ăn uống cho cổng 012. Chủ dự án chốt 2026-11-12: {@code averageWaterMl} là tổng lượng nước từng ngày
 * chia số ngày có ít nhất một lần ghi (ngày không ghi không tính), rỗng khi không có ngày nào; {@code waterDays} là số ngày
 * có dữ liệu. Bỏ tỷ lệ ăn (duyệt 2026-11-09, 012 FR-046).
 */
public record EatingDrinkingSummary(Optional<BigDecimal> averageWaterMl, int waterDays) {}
