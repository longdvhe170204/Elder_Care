package com.cambae.backend.common.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

/**
 * Chỗ lưu tệp trong bộ nhớ CHỈ cho profile {@code demo} khi chưa cấu hình bucket (001 T004): để dùng thử giao diện tải
 * tệp trên máy cá nhân khi chưa có tài khoản AWS. Tệp mất khi khởi động lại; KHÔNG phải kiểm thử với S3 thật và không
 * dùng ở {@code prod}. Có {@code ELDERCARE_S3_BUCKET} thì dùng S3 thật ({@link FileStorageConfig}).
 */
@Configuration
@Profile("demo")
class DemoFileStorageConfig {

    @Bean
    @ConditionalOnExpression("'${eldercare.file.bucket:${ELDERCARE_S3_BUCKET:}}'.isEmpty()")
    S3Client demoS3Client() {
        return new InMemoryS3Client();
    }

    private static final class InMemoryS3Client implements S3Client {

        private final Map<String, byte[]> objects = new ConcurrentHashMap<>();

        @Override
        public PutObjectResponse putObject(PutObjectRequest request, RequestBody body) {
            try (var in = body.contentStreamProvider().newStream()) {
                objects.put(request.key(), in.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return PutObjectResponse.builder().build();
        }

        @Override
        public ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest request) {
            byte[] bytes = objects.get(request.key());
            if (bytes == null) {
                throw NoSuchKeyException.builder().message("Không có đối tượng").build();
            }
            return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), bytes);
        }

        @Override
        public DeleteObjectResponse deleteObject(DeleteObjectRequest request) {
            objects.remove(request.key());
            return DeleteObjectResponse.builder().build();
        }

        @Override
        public String serviceName() {
            return "s3";
        }

        @Override
        public void close() {}
    }
}
