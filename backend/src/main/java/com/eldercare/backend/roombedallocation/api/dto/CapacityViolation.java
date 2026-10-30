package com.eldercare.backend.roombedallocation.api.dto;

/** Kiểu trao đổi CapacityViolation, hợp đồng 003 mục 1.2–1.9 (T003). */
public record CapacityViolation(SessionDate session, int capacity, int remaining, String code) {}
