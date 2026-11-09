package com.eldercare.backend.care.internal.session.domain;

/** 014 T008 ({@code dang_ky_buoi}, data-model mục 4): một dòng {@code activity_registration}. */
public record Registration(
        long id, long sessionId, long residentId, String source, Long registeredByStaffId, String status) {
    public static final String REGULAR = "REGULAR";
    public static final String MANUAL = "MANUAL";
    public static final String ADDED_AT_ATTENDANCE = "ADDED_AT_ATTENDANCE";

    public static final String REGISTERED = "REGISTERED";
    public static final String CANCELLED = "CANCELLED";
    public static final String ATTENDED = "ATTENDED";

    public static final String AUTO_TEMPLATE_CHANGED = "TEMPLATE_CHANGED";
    public static final String AUTO_SESSION_CHANGED = "SESSION_CHANGED";
    public static final String AUTO_SESSION_CANCELLED = "SESSION_CANCELLED";
    public static final String AUTO_ISOLATION = "ISOLATION";
    public static final String AUTO_INFECTION = "INFECTION_SUSPECTED";
    public static final String AUTO_FINAL_STATUS = "FINAL_STATUS";
}
