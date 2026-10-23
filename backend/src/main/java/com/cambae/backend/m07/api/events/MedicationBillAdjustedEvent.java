package com.cambae.backend.m07.api.events;

import java.math.BigDecimal;

/**
 * Sự kiện điều chỉnh tính phí thuốc sau đính chính hoặc nhận lại thuốc (SuKienTinhPhiThuocDaDieuChinh).
 * Module M11 (010) lắng nghe.
 */
public record MedicationBillAdjustedEvent(
        BilledSource source, long sourceId, BigDecimal newQuantity, BillAdjustmentReason reason) {}
