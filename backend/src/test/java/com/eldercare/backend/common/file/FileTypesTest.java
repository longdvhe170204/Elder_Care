package com.eldercare.backend.common.file;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.document.ExcelDocument;
import com.eldercare.backend.common.document.ExcelDocument.Column;
import com.eldercare.backend.common.document.ExcelDocument.Format;
import com.eldercare.backend.common.document.PdfDocument;
import com.eldercare.backend.common.error.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 017 T003 (research E-11; 001 research H-13): {@code FileApi.upload} nhận thêm {@code .xlsx} cho tệp do hệ thống tạo;
 * {@code POST /files} vẫn chỉ PDF, JPEG, PNG; tối đa 10 MB; kiểu khai báo phải khớp nội dung.
 */
class FileTypesTest {

    private static final byte[] PDF =
            PdfDocument.a4(null, null, "Tiêu đề").paragraph("Nội dung").toBytes();
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0};

    private static byte[] xlsx() {
        ExcelDocument excel = new ExcelDocument();
        excel.sheet("Sao kê").table(List.of(new Column("Mô tả", Format.TEXT)), List.of(List.of("Nộp tiền")));
        return excel.toBytes();
    }

    @Test
    void systemGenerated_acceptsXlsx_andUserTypes() {
        assertThatCode(() -> FileTypes.check(FileTypes.XLSX, xlsx(), FileTypes.SYSTEM_GENERATED))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileTypes.check("application/pdf", PDF, FileTypes.SYSTEM_GENERATED))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileTypes.check("image/png", PNG, FileTypes.USER_UPLOAD))
                .doesNotThrowAnyException();
        assertThatCode(() -> FileTypes.check("image/jpeg", JPEG, FileTypes.USER_UPLOAD))
                .doesNotThrowAnyException();
    }

    @Test
    void userUpload_rejectsXlsx() {
        assertThatThrownBy(() -> FileTypes.check(FileTypes.XLSX, xlsx(), FileTypes.USER_UPLOAD))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.errorCode())
                                .isEqualTo("FILE_TYPE_NOT_ALLOWED"));
    }

    @Test
    void declaredTypeNotMatchingContent_rejected() {
        assertThatThrownBy(() -> FileTypes.check("application/pdf", PNG, FileTypes.USER_UPLOAD))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.errorCode())
                                .isEqualTo("FILE_TYPE_NOT_ALLOWED"));
        assertThatThrownBy(() -> FileTypes.check(FileTypes.XLSX, PDF, FileTypes.SYSTEM_GENERATED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void over10Mb_rejected() {
        byte[] big = new byte[(int) FileTypes.MAX_SIZE + 1];
        System.arraycopy(PDF, 0, big, 0, PDF.length);

        assertThatThrownBy(() -> FileTypes.check("application/pdf", big, FileTypes.SYSTEM_GENERATED))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.errorCode())
                                .isEqualTo("FILE_TOO_LARGE"));
    }
}
