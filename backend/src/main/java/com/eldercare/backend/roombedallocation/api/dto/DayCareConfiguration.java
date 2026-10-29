package com.eldercare.backend.roombedallocation.api.dto;

import java.util.List;

/** Kiểu trao đổi DayCareConfiguration, hợp đồng 003 mục 1.2–1.9 (T003). */
public record DayCareConfiguration(long id, LocationRef location, int capacity, List<DayCareSession> sessions) {}
