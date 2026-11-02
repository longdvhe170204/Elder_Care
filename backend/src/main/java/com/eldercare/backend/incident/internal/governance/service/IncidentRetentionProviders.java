package com.eldercare.backend.incident.internal.governance.service;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lưu giữ dữ liệu cho M05 theo R-11 (data-model mục 14; FR-050).
 */
@Configuration
public class IncidentRetentionProviders {

    public static final String INCIDENT = "M05.INCIDENT";
    public static final String ALERT = "M05.ALERT";
    public static final String CONTAINMENT = "M05.CONTAINMENT";

    @Bean
    public RetentionProvider incidentRetention() {
        return incident();
    }

    @Bean
    public RetentionProvider alertRetention() {
        return alert();
    }

    @Bean
    public RetentionProvider containmentRetention() {
        return containment();
    }

    public static RetentionProvider incident() {
        return new BaseRetentionProvider(INCIDENT, new RetentionKind.ResidentRecord());
    }

    public static RetentionProvider alert() {
        return new BaseRetentionProvider(ALERT, new RetentionKind.ResidentRecord());
    }

    public static RetentionProvider containment() {
        return new BaseRetentionProvider(CONTAINMENT, new RetentionKind.NotResidentLinked());
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
