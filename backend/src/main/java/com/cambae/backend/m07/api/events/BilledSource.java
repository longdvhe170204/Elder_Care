package com.cambae.backend.m07.api.events;

/**
 * Nguồn phát sinh tính phí thuốc (LIEU | LAN_DUNG_PRN | LAN_GIAO).
 */
public enum BilledSource {
    DOSE, // Liều định kỳ
    PRN_ADMINISTRATION, // Lần dùng PRN
    DISPENSE_RECORD // Lần giao thuốc mang theo
}
