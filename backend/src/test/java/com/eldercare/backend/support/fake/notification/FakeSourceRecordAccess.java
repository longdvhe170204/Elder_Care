package com.eldercare.backend.support.fake.notification;

import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Bản giả {@link SourceRecordAccessPort} cho loại nguồn {@code M05.TEST} của test 009: mở được các mã trong {@link
 * #allow}, mã {@link #FAILING} ném lỗi (009 phải coi như không mở được). Thêm vào context bằng {@code
 * @Import(FakeSourceRecordAccess.Config.class)}.
 */
public class FakeSourceRecordAccess implements SourceRecordAccessPort {

    public static final String FAILING = "failing";

    private final Set<String> openable = new HashSet<>();

    public FakeSourceRecordAccess allow(String sourceRecordId) {
        openable.add(sourceRecordId);
        return this;
    }

    public void reset() {
        openable.clear();
    }

    @Override
    public String sourceRecordType() {
        return "M05.TEST";
    }

    @Override
    public boolean canOpen(String sourceRecordId) {
        if (FAILING.equals(sourceRecordId)) {
            throw new IllegalStateException("Module nguồn lỗi");
        }
        return openable.contains(sourceRecordId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @Bean
        FakeSourceRecordAccess fakeSourceRecordAccess() {
            return new FakeSourceRecordAccess();
        }
    }
}
