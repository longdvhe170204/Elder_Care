package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.billing.internal.balance.document.domain.VietQrPayload;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 017 T008 phần QR (research E-04, E-13): chuỗi EMVCo VietQR chuyển nhanh tới tài khoản, CRC16-CCITT. */
class VietQrPayloadTest {

    @Test
    void E_04_crc16CcittMatchesTheStandardCheckValue() {
        // giá trị kiểm chuẩn của CRC-16/CCITT-FALSE (poly 0x1021, init 0xFFFF) cho "123456789"
        assertThat(VietQrPayload.crc16("123456789")).isEqualTo("29B1");
    }

    @Test
    void E_04_buildsTheEmvcoStringForAnAccountWithPaymentCode() {
        // chuỗi dựng độc lập bằng script node (TLV, độ dài hai chữ số, CRC16-CCITT bit từng bit), không dùng mã đang
        // kiểm
        assertThat(VietQrPayload.of("970415", "113366668888", "NCT7K2M9X4A"))
                .isEqualTo("00020101021138560010A0000007270126000697041501121133666688880208QRIBFTTA"
                        + "53037045802VN62150811NCT7K2M9X4A6304FD69");
        assertThat(VietQrPayload.of("970436", "0011004000123", "NCT123456Z"))
                .isEqualTo("00020101021138570010A00000072701270006970436011300110040001230208QRIBFTTA"
                        + "53037045802VN62140810NCT123456Z6304658F");
    }

    @Test
    void E_04_trailingCrcCoversEverythingBeforeIt() {
        String payload = VietQrPayload.of("970436", "0011004000123", "NCT123456Z");

        String body = payload.substring(0, payload.length() - 4);
        assertThat(body).endsWith("6304");
        assertThat(payload.substring(payload.length() - 4)).isEqualTo(VietQrPayload.crc16(body));
        assertThat(payload)
                .startsWith("000201")
                .contains("010211")
                .contains("A000000727")
                .contains("QRIBFTTA");
        // không có trường số tiền (tag 54): duyệt các tag cấp một
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < payload.length(); ) {
            tags.add(payload.substring(i, i + 2));
            i += 4 + Integer.parseInt(payload.substring(i + 2, i + 4));
        }
        assertThat(tags).containsExactly("00", "01", "38", "53", "58", "62", "63");
    }

    @Test
    void E_04_rejectsInvalidInputs() {
        assertThatThrownBy(() -> VietQrPayload.of("97041", "123456", "NCT1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VietQrPayload.of("970415", "12AB", "NCT1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VietQrPayload.of("970415", "123456", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
