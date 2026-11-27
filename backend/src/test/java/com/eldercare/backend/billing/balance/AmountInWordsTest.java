package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.balance.internal.document.domain.AmountInWords;
import org.junit.jupiter.api.Test;

/** Q-460: số tiền bằng chữ trên biên nhận nộp tiền. */
class AmountInWordsTest {

    private static void reads(long amount, String words) {
        assertThat(AmountInWords.dong(amount)).as("%d", amount).isEqualTo(words);
    }

    @Test
    void unitsAndTens() {
        reads(1, "Một đồng");
        reads(10, "Mười đồng");
        reads(15, "Mười lăm đồng");
        reads(21, "Hai mươi mốt đồng");
        reads(24, "Hai mươi bốn đồng");
        reads(105, "Một trăm linh năm đồng");
        reads(311, "Ba trăm mười một đồng");
    }

    @Test
    void thousandsMillionsBillions() {
        reads(2_500_000, "Hai triệu năm trăm nghìn đồng");
        reads(1_000_005, "Một triệu không trăm linh năm đồng");
        reads(10_000_000, "Mười triệu đồng");
        reads(2_249_991, "Hai triệu hai trăm bốn mươi chín nghìn chín trăm chín mươi mốt đồng");
        reads(1_000_000_000, "Một tỷ đồng");
        reads(1_001_000_000_000L, "Một nghìn không trăm linh một tỷ đồng");
    }
}
