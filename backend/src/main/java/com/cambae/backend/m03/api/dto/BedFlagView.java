package com.cambae.backend.m03.api.dto;

import java.util.Set;

/** Kiểu trao đổi BedFlagView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record BedFlagView(Long residentId, long bedId, Set<String> flags) {}
