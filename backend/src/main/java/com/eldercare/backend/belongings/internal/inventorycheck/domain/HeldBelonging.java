package com.eldercare.backend.belongings.internal.inventorycheck.domain;

import java.math.BigDecimal;

/** Một đồ có giá trị Đang giữ tại vị trí lúc lập phiếu kiểm kê (FR-028a). */
public record HeldBelonging(long id, long residentId, BigDecimal amount, long conditionLevelId) {}
