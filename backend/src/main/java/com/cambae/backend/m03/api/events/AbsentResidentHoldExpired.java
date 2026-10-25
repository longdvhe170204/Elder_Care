package com.cambae.backend.m03.api.events;
/** AbsentResidentHoldExpired: hợp đồng 003 mục 1.11; không gửi thông báo trực tiếp. */
public record AbsentResidentHoldExpired(long bedId, long residentId) {}
