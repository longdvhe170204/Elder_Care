package com.eldercare.backend.resident.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Bệnh viện ưu tiên trên phiếu nguyện vọng khi điều trị (FR-023j). */
@Schema(requiredProperties = {"name", "address"})
public record PreferredHospital(String name, String address) {}
