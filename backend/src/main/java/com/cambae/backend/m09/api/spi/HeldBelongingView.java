package com.cambae.backend.m09.api.spi;

/** Đồ Đang giữ, nhân viên giữ, người cao tuổi và vị trí lưu giữ; vị trí có thể chưa có (013 data-model). */
public record HeldBelongingView(long belongingId, long holderStaffId, long residentId, Long storageLocationId) {}
