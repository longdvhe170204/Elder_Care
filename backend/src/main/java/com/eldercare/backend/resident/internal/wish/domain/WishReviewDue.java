package com.eldercare.backend.resident.internal.wish.domain;

/** Phiếu Hiệu lực đã tới ngày xem lại nhận định và chưa được nhắc (FR-023i). */
public record WishReviewDue(long residentId, long wishId, long assessingDoctorId) {}
