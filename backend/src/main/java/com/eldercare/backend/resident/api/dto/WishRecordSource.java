package com.eldercare.backend.resident.api.dto;

/** Nguồn của một phiên bản phiếu nguyện vọng; CRITICAL_RECONFIRMATION do 007 gọi (BR-M05-16). */
public enum WishRecordSource {
    ADMISSION,
    FAMILY_REQUEST,
    CRITICAL_RECONFIRMATION;
}
