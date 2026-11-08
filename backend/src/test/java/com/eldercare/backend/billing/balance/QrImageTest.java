package com.eldercare.backend.billing.balance;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.balance.document.domain.VietQrPayload;
import com.eldercare.backend.billing.internal.balance.document.service.QrImage;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** 017 research E-04: ảnh QR bằng ZXing chèn vào phiếu báo phí; ảnh đọc ngược ra đúng chuỗi VietQR. */
class QrImageTest {

    @Test
    void E_04_pngDecodesBackToThePayload() throws Exception {
        String payload = VietQrPayload.of("970415", "113366668888", "NCT7K2M9X4A");

        byte[] png = QrImage.png(payload, 300);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(300);
        assertThat(image.getHeight()).isEqualTo(300);
        int[] pixels = image.getRGB(0, 0, 300, 300, null, 0, 300);
        LuminanceSource source = new RGBLuminanceSource(300, 300, pixels);
        String decoded = new QRCodeReader()
                .decode(new BinaryBitmap(new HybridBinarizer(source)))
                .getText();
        assertThat(decoded).isEqualTo(payload);
    }
}
