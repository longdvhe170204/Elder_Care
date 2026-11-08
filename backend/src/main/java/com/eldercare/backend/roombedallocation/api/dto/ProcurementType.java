package com.eldercare.backend.roombedallocation.api.dto;

/** 019 glossary 25.4: loại yêu cầu mua sắm (FR-008, FR-008d); M11 dùng chung kiểu này với SPI của M03. */
public enum ProcurementType {
    INGREDIENT,
    ASSET,
    SERVICE,
    MEDICATION,
    OTHER
}
