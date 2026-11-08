package com.eldercare.backend.billing.internal.balance.document.domain;

import com.eldercare.backend.common.time.BusinessTime;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Cách in số tiền, ngày, giờ trên chứng từ của 017: tiền theo đồng, dấu chấm ngăn nhóm; ngày giờ theo giờ Việt Nam. */
public final class DocumentFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DocumentFormat() {}

    /** {@code 1500000} thành {@code 1.500.000}; số âm giữ dấu trừ. */
    public static String money(long amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", symbols).format(amount);
    }

    public static String date(LocalDate date) {
        return DATE.format(date);
    }

    public static String dateTime(Instant instant) {
        return DATE_TIME.format(instant.atZone(BusinessTime.ZONE));
    }

    public static String date(Instant instant) {
        return DATE.format(instant.atZone(BusinessTime.ZONE));
    }
}
