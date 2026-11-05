package com.eldercare.backend.incident.internal.emergency.domain;

import java.util.List;

/** Nội dung bản chụp thẻ khẩn cấp: thẻ và người đã được hiển thị (data-model {@code nguoi_duoc_hien_thi}). */
public record EmergencyCardSnapshot(EmergencyCard card, List<Long> shownTo) {}
