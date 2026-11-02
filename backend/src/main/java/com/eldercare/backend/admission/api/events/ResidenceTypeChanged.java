package com.eldercare.backend.admission.api.events;

/** Loại lưu trú của người cao tuổi đã đổi do phụ lục áp dụng; 003 nghe để đánh giá lại phòng (003 contracts mục 2.2; T004, T045). */
public record ResidenceTypeChanged(long residentId) {}
