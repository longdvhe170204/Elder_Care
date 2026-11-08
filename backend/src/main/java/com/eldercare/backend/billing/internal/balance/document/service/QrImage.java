package com.eldercare.backend.billing.internal.balance.document.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.imageio.ImageIO;

/** Ảnh PNG mã QR bằng ZXing core (017 research E-04), để chèn vào phiếu báo phí dựng ở máy chủ. */
public final class QrImage {

    private QrImage() {}

    /** Ảnh vuông {@code size} điểm ảnh, nền trắng, mức sửa lỗi M. */
    public static byte[] png(String content, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(
                            content,
                            BarcodeFormat.QR_CODE,
                            size,
                            size,
                            Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 2));
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IllegalArgumentException("Không dựng được mã QR", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
