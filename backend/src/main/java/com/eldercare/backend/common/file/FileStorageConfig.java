package com.eldercare.backend.common.file;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Máy khách S3 thật (001 research H-13): chỉ tạo khi có tên bucket ({@code ELDERCARE_S3_BUCKET}); region
 * ({@code AWS_REGION}) và thông tin đăng nhập theo chuỗi mặc định của SDK, không ghi vào kho mã. Không có thì
 * {@link FileService} báo 503 {@code FILE_STORAGE_UNAVAILABLE}.
 */
@Configuration
class FileStorageConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnExpression("!'${eldercare.file.bucket:${ELDERCARE_S3_BUCKET:}}'.isEmpty()")
    S3Client s3Client() {
        return S3Client.create();
    }

    /**
     * Mặc định của Spring chỉ nhận 1 MB; tệp tối đa {@link FileTypes#MAX_SIZE} (10 MB). Vượt thì {@code FileController}
     * trả 422 {@code FILE_TOO_LARGE}.
     */
    @Bean
    MultipartConfigElement multipartConfig() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofBytes(FileTypes.MAX_SIZE));
        factory.setMaxRequestSize(
                DataSize.ofBytes(FileTypes.MAX_SIZE + DataSize.ofMegabytes(1).toBytes()));
        return factory.createMultipartConfig();
    }
}
