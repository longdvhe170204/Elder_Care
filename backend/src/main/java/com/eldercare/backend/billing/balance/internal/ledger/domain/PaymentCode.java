package com.eldercare.backend.billing.balance.internal.ledger.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Mã nộp tiền (research E-04; FR-001): {@code NCT} + 6 ký tự Base32 Crockford + 1 ký tự kiểm tra (giá trị 6 ký tự mod
 * 37, bảng ký hiệu kiểm tra Crockford). Mã sinh ngẫu nhiên, thử lại khi trùng; chỉ nhận mã có ký tự kiểm tra là chữ, số
 * (bỏ mã rơi vào {@code * ~ $ =}) vì nội dung chuyển khoản của ngân hàng thường bỏ ký tự đặc biệt.
 */
public final class PaymentCode {

    private static final String PREFIX = "NCT";
    private static final String SYMBOLS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private static final String CHECK_SYMBOLS = SYMBOLS + "*~$=U";
    private static final int LENGTH = 6;

    private PaymentCode() {}

    public static String generate(RandomGenerator random) {
        while (true) {
            StringBuilder body = new StringBuilder(LENGTH);
            for (int i = 0; i < LENGTH; i++) body.append(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
            char check = check(body);
            if (Character.isLetterOrDigit(check)) return PREFIX + body + check;
        }
    }

    /** Mã đúng dạng từ chữ người dùng gõ (hoa thường, khoảng trắng, gạch nối); rỗng khi sai dạng hoặc sai ký tự kiểm tra. */
    public static Optional<String> normalize(String typed) {
        if (typed == null) return Optional.empty();
        String code = typed.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (code.length() != PREFIX.length() + LENGTH + 1 || !code.startsWith(PREFIX)) return Optional.empty();
        String body = code.substring(PREFIX.length(), PREFIX.length() + LENGTH);
        for (char c : body.toCharArray()) if (SYMBOLS.indexOf(c) < 0) return Optional.empty();
        return code.charAt(code.length() - 1) == check(body) ? Optional.of(code) : Optional.empty();
    }

    private static char check(CharSequence body) {
        long value = 0;
        for (int i = 0; i < body.length(); i++) value = value * 32 + SYMBOLS.indexOf(body.charAt(i));
        return CHECK_SYMBOLS.charAt((int) (value % 37));
    }
}
