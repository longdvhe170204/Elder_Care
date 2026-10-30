package com.eldercare.backend.resident.api.dto;

import java.time.LocalDate;

/** Bảo hiểm y tế, không bắt buộc (FR-023j); căn cứ xử lý dữ liệu cá nhân chờ đối chiếu 24.4 (Q-384). */
public record HealthInsurance(String initialRegistrationPlace, String cardNumber, LocalDate cardExpiry) {}
