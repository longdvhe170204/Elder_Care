package com.cambae.backend.m11.api.events;

import java.math.BigDecimal;
import java.util.Optional;

/** FR-020: phân biệt hủy nguồn với số lượng mới bằng 0; giữ khóa sự kiện của nguồn. */
public record CostSourceChanged(
        String sourceType,
        String sourceId,
        String unit,
        Optional<BigDecimal> newQuantity,
        boolean cancelled,
        String basis,
        String eventKey) {}
