package com.cambae.backend.m15.parameter.api;

/** Kiểu giá trị tham số, suy từ đơn vị của giá trị mặc định ở Phụ lục 25 (research R-07, Q-315). */
public enum ParameterKind {
    DURATION,
    INTEGER,
    PERCENTAGE,
    MONEY,
    TIME_OF_DAY,
    TABLE
}
