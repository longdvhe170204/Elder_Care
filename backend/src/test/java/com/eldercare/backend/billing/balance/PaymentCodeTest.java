package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.ledger.domain.PaymentCode;
import com.eldercare.backend.support.TableRows;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * 017 T010 (research E-04; FR-001, FR-007): mã nộp tiền {@code NCT} + 6 ký tự Base32 Crockford + 1 ký tự kiểm tra mod
 * 37; tìm theo mã không phân biệt hoa thường, bỏ khoảng trắng, gạch nối; ký tự kiểm tra sai thì không nhận.
 */
class PaymentCodeTest {

    @Test
    void E_04_generatedCodes_carryAValidCheckCharacter_andStayAlphanumeric() {
        Random random = new Random(17);
        IntStream.range(0, 2_000).mapToObj(i -> PaymentCode.generate(random)).forEach(code -> {
            assertThat(code).matches("NCT[0-9A-HJKMNP-TV-Z]{6}[0-9A-HJKMNP-Z]");
            assertThat(PaymentCode.normalize(code)).contains(code);
        });
    }

    @Test
    void E_04_normalize_acceptsTypedVariants_andRejectsWrongCheckCharacter() {
        String code = PaymentCode.generate(new Random(3));
        char wrong = code.charAt(9) == '0' ? '1' : '0';
        TableRows.csv(
                row -> assertThat(PaymentCode.normalize(row[0])).isEqualTo(Optional.ofNullable(row[1])),
                code + ", " + code,
                code.toLowerCase() + ", " + code,
                code.substring(0, 3) + " " + code.substring(3, 6) + "-" + code.substring(6) + ", " + code,
                code.substring(0, 9) + wrong + ",",
                "NCT12345,",
                "ABC1234567,");
    }
}
