package com.eldercare.backend.billing.balance.internal.document.domain;

import java.util.ArrayList;
import java.util.List;

/** Số tiền đồng đọc bằng chữ tiếng Việt cho chứng từ (Q-460); chỉ nhận số dương. */
public final class AmountInWords {

    private static final String[] DIGITS = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
    private static final long BILLION = 1_000_000_000L;

    private AmountInWords() {}

    public static String dong(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0");
        }
        String words = String.join(" ", read(amount, false));
        return Character.toUpperCase(words.charAt(0)) + words.substring(1) + " đồng";
    }

    /** {@code padded}: đã có phần lớn hơn đứng trước nên nhóm thiếu hàng trăm phải đọc "không trăm". */
    private static List<String> read(long n, boolean padded) {
        var words = new ArrayList<String>();
        if (n >= BILLION) {
            words.addAll(read(n / BILLION, padded));
            words.add("tỷ");
            n %= BILLION;
            padded = true;
        }
        int[] groups = {(int) (n / 1_000_000), (int) (n / 1_000 % 1_000), (int) (n % 1_000)};
        String[] scales = {"triệu", "nghìn", ""};
        for (int i = 0; i < groups.length; i++) {
            if (groups[i] == 0) continue;
            words.addAll(group(groups[i], padded));
            if (!scales[i].isEmpty()) words.add(scales[i]);
            padded = true;
        }
        return words;
    }

    private static List<String> group(int n, boolean padded) {
        int hundreds = n / 100;
        int tens = n / 10 % 10;
        int units = n % 10;
        var words = new ArrayList<String>();
        boolean saidHundreds = hundreds > 0 || padded;
        if (saidHundreds) {
            words.add(DIGITS[hundreds]);
            words.add("trăm");
        }
        if (tens == 0) {
            if (units > 0 && saidHundreds) words.add("linh");
        } else if (tens == 1) {
            words.add("mười");
        } else {
            words.add(DIGITS[tens]);
            words.add("mươi");
        }
        if (units == 0) return words;
        if (units == 1 && tens > 1) words.add("mốt");
        else if (units == 5 && tens > 0) words.add("lăm");
        else words.add(DIGITS[units]);
        return words;
    }
}
