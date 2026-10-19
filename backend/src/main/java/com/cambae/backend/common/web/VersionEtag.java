package com.cambae.backend.common.web;

import com.cambae.backend.common.error.VersionMismatchException;

/** ETag là số phiên bản; lệnh sửa so If-Match với phiên bản hiện tại (FR-021, FR-040; research R-03). */
public final class VersionEtag {

    private VersionEtag() {}

    /** Giá trị header ETag, ví dụ {@code "3"}. */
    public static String of(long version) {
        return "\"" + version + "\"";
    }

    /** Ném {@link VersionMismatchException} kèm {@code currentValue} khi If-Match không phải phiên bản hiện tại. */
    public static void requireMatch(String ifMatch, long currentVersion, Object currentValue) {
        if (ifMatch == null || parse(ifMatch) != currentVersion) {
            throw new VersionMismatchException(currentValue);
        }
    }

    private static long parse(String ifMatch) {
        String value = ifMatch.strip();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        value = value.replace("\"", "");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
