package com.cambae.backend.common.file;

import com.cambae.backend.common.error.BusinessException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * Kiểu và kích thước tệp nhận được (001 research H-13; 017 research E-11). {@code POST /files} chỉ nhận
 * {@link #USER_UPLOAD}; {@link FileApi#upload} nhận {@link #SYSTEM_GENERATED} (thêm {@code .xlsx} cho chứng từ, file
 * xuất do hệ thống tạo). Kiểu khai báo phải khớp chữ ký đầu nội dung.
 */
public final class FileTypes {

    public static final long MAX_SIZE = 10L * 1024 * 1024;
    public static final String PDF = "application/pdf";
    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Tệp người dùng tải lên qua {@code POST /files}. */
    public static final Set<String> USER_UPLOAD = Set.of(PDF, JPEG, PNG);

    /** Mọi tệp qua {@link FileApi#upload}, gồm tệp hệ thống tạo. */
    public static final Set<String> SYSTEM_GENERATED = Set.of(PDF, JPEG, PNG, XLSX);

    // .xlsx là tệp zip
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            PDF, new byte[] {'%', 'P', 'D', 'F', '-'},
            JPEG, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            PNG, new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'},
            XLSX, new byte[] {'P', 'K', 3, 4});

    private FileTypes() {}

    /** Ném 422 {@code FILE_TYPE_NOT_ALLOWED} hoặc {@code FILE_TOO_LARGE} nếu tệp không nhận được. */
    public static void check(String mimeType, byte[] content, Set<String> allowed) {
        if (mimeType == null || !allowed.contains(mimeType) || !startsWith(content, SIGNATURES.get(mimeType))) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "FILE_TYPE_NOT_ALLOWED", "Kiểu tệp không được nhận");
        }
        if (content.length > MAX_SIZE) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "FILE_TOO_LARGE", "Tệp quá 10 MB");
        }
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        return content.length >= signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }
}
