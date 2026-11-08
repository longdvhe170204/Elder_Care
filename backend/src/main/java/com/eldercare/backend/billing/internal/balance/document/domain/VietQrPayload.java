package com.eldercare.backend.billing.internal.balance.document.domain;

import java.nio.charset.StandardCharsets;

/**
 * Chuỗi mã QR VietQR chuyển nhanh tới tài khoản (017 research E-04, FR-003): EMVCo, merchant account {@code A000000727},
 * dịch vụ {@code QRIBFTTA}, tiền tệ VND, không có trường số tiền, nội dung chuyển khoản là mã nộp tiền, CRC16-CCITT ở
 * cuối. Điểm khởi tạo đặt {@code 11} (mã tĩnh) vì không mang số tiền. Hàm thuần; ảnh QR vẽ ở client hoặc bằng ZXing khi
 * dựng PDF.
 */
public final class VietQrPayload {

    private VietQrPayload() {}

    /**
     * @param bin mã BIN ngân hàng (6 chữ số, CFG-M11-10)
     * @param accountNumber số tài khoản (chỉ chữ số, 6 → 19 ký tự)
     * @param content nội dung chuyển khoản, tức mã nộp tiền
     */
    public static String of(String bin, String accountNumber, String content) {
        if (bin == null || !bin.matches("\\d{6}")) {
            throw new IllegalArgumentException("Mã BIN gồm 6 chữ số");
        }
        if (accountNumber == null || !accountNumber.matches("\\d{6,19}")) {
            throw new IllegalArgumentException("Số tài khoản gồm 6 đến 19 chữ số");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Thiếu nội dung chuyển khoản");
        }
        String account = field("00", bin) + field("01", accountNumber);
        String merchant = field("00", "A000000727") + field("01", account) + field("02", "QRIBFTTA");
        String body = field("00", "01")
                + field("01", "11")
                + field("38", merchant)
                + field("53", "704")
                + field("58", "VN")
                + field("62", field("08", content))
                + "6304";
        return body + crc16(body);
    }

    /** CRC-16/CCITT-FALSE (đa thức 0x1021, khởi tạo 0xFFFF), bốn ký tự hex in hoa. */
    public static String crc16(String data) {
        int crc = 0xFFFF;
        for (byte b : data.getBytes(StandardCharsets.US_ASCII)) {
            crc ^= (b & 0xFF) << 8;
            for (int bit = 0; bit < 8; bit++) {
                crc = (crc & 0x8000) != 0 ? (crc << 1) ^ 0x1021 : crc << 1;
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc);
    }

    private static String field(String tag, String value) {
        return tag + String.format("%02d", value.length()) + value;
    }
}
