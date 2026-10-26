package com.cambae.backend.m03.api.dto;
/** Mã BedStatus theo hợp đồng 003; tên logic ánh xạ ở glossary mục 9. */
public enum BedStatus {
    EMPTY,
    OCCUPIED,
    HELD,
    AWAITING_READY,
    UNDER_MAINTENANCE,
    SUSPENDED,
    RETIRED
}
