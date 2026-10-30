package com.eldercare.backend.notification.internal.persistence;

/**
 * Đoạn SQL đọc dòng loại M13 ở {@code record_event} (009 T043; research R-15 mục 4: module đọc bằng SQL dòng loại có tiền
 * tố của mình, mọi lệnh ghi qua API). Tham số là bí danh bảng trong câu.
 */
public final class M13Sql {

    private M13Sql() {}

    /** Lần gửi của thông báo {@code <a>.entity_id}. */
    public static String attempts(String a) {
        return a + ".kind = 'M13.DELIVERY_ATTEMPT' AND " + a + ".entity_type = 'M13.NOTIFICATION'";
    }

    /** Kết quả {@code r} của lần gửi {@code a}. */
    public static String resultOf(String r, String a) {
        return r + ".kind = 'M13.DELIVERY_RESULT' AND " + r + ".entity_type = 'M13.DELIVERY_ATTEMPT' AND " + r
                + ".entity_id = " + a + ".id";
    }

    /** Sự kiện của thông báo {@code <e>.entity_id}. */
    public static String notificationEvents(String e) {
        return e + ".kind = 'M13.NOTIFICATION_EVENT' AND " + e + ".entity_type = 'M13.NOTIFICATION'";
    }

    public static String channel(String a) {
        return "JSON_VALUE(" + a + ".content, '$.channel')";
    }

    public static String attemptNo(String a) {
        return "JSON_VALUE(" + a + ".content, '$.attemptNo' RETURNING SIGNED)";
    }

    /** Lần gửi {@code a} là lần mới nhất của kênh đó trong thông báo đó. */
    public static String latestOfChannel(String a) {
        return attemptNo(a) + " = (SELECT MAX(" + attemptNo("x") + ") FROM record_event x WHERE " + attempts("x")
                + " AND x.entity_id = " + a + ".entity_id AND " + channel("x") + " = " + channel(a) + ")";
    }
}
