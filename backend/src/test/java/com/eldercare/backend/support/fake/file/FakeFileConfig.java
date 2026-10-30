package com.eldercare.backend.support.fake.file;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả nơi lưu tệp của {@code common.file} (001 T003): test cần tệp thì {@code @Import(FakeFileConfig.class)} rồi
 * tiêm {@link InMemoryS3Client} để kiểm đối tượng đã lưu. Không import thì {@code FileService} báo 503
 * {@code FILE_STORAGE_UNAVAILABLE} (không có {@code S3Client}).
 */
@TestConfiguration
public class FakeFileConfig {

    @Bean
    @Primary
    InMemoryS3Client inMemoryS3Client() {
        return new InMemoryS3Client();
    }
}
