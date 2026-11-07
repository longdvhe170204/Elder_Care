package com.eldercare.backend.support.fake.billing;

import com.eldercare.backend.billing.api.spi.ProcurementPrescriptionPort;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 019 khai {@link ProcurementPrescriptionPort}, chủ fake là C; bản thật do D (006) cài ở M07. Mặc định không có đơn nào;
 * test đặt đơn nguồn viện chưa kết thúc bằng {@link #facility}.
 */
public class FakeProcurementPrescriptions implements ProcurementPrescriptionPort {
    private final Map<Long, Set<Long>> unended = new HashMap<>();

    public FakeProcurementPrescriptions facility(long residentId, long prescriptionId) {
        unended.computeIfAbsent(residentId, r -> new HashSet<>()).add(prescriptionId);
        return this;
    }

    @Override
    public boolean isUnendedFacilityPrescription(long residentId, long prescriptionId) {
        return unended.getOrDefault(residentId, Set.of()).contains(prescriptionId);
    }

    public void reset() {
        unended.clear();
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class Config {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FakeProcurementPrescriptions fakeProcurementPrescriptions() {
            return new FakeProcurementPrescriptions();
        }
    }
}
