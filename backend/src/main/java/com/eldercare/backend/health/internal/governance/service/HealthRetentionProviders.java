package com.eldercare.backend.health.internal.governance.service;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lưu giữ dữ liệu cho M06 theo R-11 (data-model mục 14; FR-050).
 */
@Configuration
public class HealthRetentionProviders {

    public static final String VITAL_SIGN_RECORD = "M06.VITAL_SIGN_RECORD";
    public static final String FACILITY_LICENSE = "M06.FACILITY_LICENSE";

    @Bean
    public RetentionProvider vitalSignRetention() {
        return vitalSignRecord();
    }

    @Bean
    public RetentionProvider facilityLicenseRetention() {
        return facilityLicense();
    }

    public static RetentionProvider vitalSignRecord() {
        return new BaseRetentionProvider(VITAL_SIGN_RECORD, new RetentionKind.ResidentRecord());
    }

    public static RetentionProvider facilityLicense() {
        return new BaseRetentionProvider(FACILITY_LICENSE, new RetentionKind.NotResidentLinked());
    }

    record BaseRetentionProvider(String dataType, RetentionKind retentionKind) implements RetentionProvider {
        @Override
        public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
            return Stream.empty();
        }

        @Override
        public Set<DataRef> referencesTo(long id) {
            return Set.of();
        }

        @Override
        public void delete(Collection<Long> ids) {}
    }
}
