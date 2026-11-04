package com.eldercare.backend.family.api.dto;

/** Giấy tờ tùy thân đã đăng ký của người thân ({@code GiayToView}); số giấy tờ là dữ liệu cá nhân. */
public record IdDocumentView(String type, String number) {}
