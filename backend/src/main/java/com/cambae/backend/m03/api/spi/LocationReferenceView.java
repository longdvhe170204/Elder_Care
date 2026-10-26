package com.cambae.backend.m03.api.spi;
/** Bản ghi tham chiếu gây chặn vị trí: loại, id và nhãn hiển thị (FR-005). */
public record LocationReferenceView(String type, long id, String label) {}
