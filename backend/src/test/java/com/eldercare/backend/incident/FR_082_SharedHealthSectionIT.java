package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.FamilyPortalHealthApi;
import com.eldercare.backend.incident.api.dto.FamilyIncidentView;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.resident.FakeConsentApi;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * K3 (quickstart mục 3; FR-082, Q-352, Q-358, research S-19a; T061a): sự cố cung cấp cho cổng người thân tách phần chung
 * và phần sức khỏe; phần sức khỏe chỉ khi bản đồng ý cho người thân xem sức khỏe; sự cố lây nhiễm chỉ có "có sự cố" và
 * thời điểm ở phần chung (Q-72); sự cố Đã hủy không có (FR-046a).
 */
@Import(FakeConsentApi.Config.class)
class FR_082_SharedHealthSectionIT extends IncidentHttpTestBase {
    @Autowired
    FamilyPortalHealthApi portal;

    @Autowired
    FakeConsentApi consents;

    @Autowired
    IncidentProgressionStore progressions;

    long allowed;
    long notAllowed;

    @BeforeEach
    void consent() {
        allowed = TestUsers.nextId();
        notAllowed = TestUsers.nextId();
        consents.reset();
        consents.active(resident, Set.of(allowed), true);
    }

    @Test
    void FR_082_K3_consentedRelativeGetsHealthPart_otherOnlyGeneralPart() {
        long fall = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        progressions.add(
                fall,
                progressionType(ProgressionGroup.EVALUATION),
                "TEST_EVALUATION",
                "Bác sĩ khám: bầm tay trái",
                time.now(),
                nurse.accountId(),
                false,
                floor,
                time.now());
        jdbc.update(
                "UPDATE incident SET status = 'CLOSED', resolution = 'Đã khám, theo dõi tại phòng' WHERE id = ?", fall);

        FamilyIncidentView seen = only(portal.incidents(resident, allowed, from(), to()));
        assertThat(seen.incidentId()).isEqualTo(fall);
        assertThat(seen.general().typeName()).isEqualTo("Ngã");
        assertThat(seen.general().level()).isEqualTo("MEDIUM");
        assertThat(seen.general().processingStatus()).isEqualTo("RESOLVED");
        assertThat(seen.general().occurredAt()).isNotNull();
        assertThat(seen.health()).hasValueSatisfying(health -> {
            assertThat(health.description()).isEqualTo("Mô tả sự cố");
            assertThat(health.initialResponse()).isNull();
            assertThat(health.progressions()).containsExactly("Bác sĩ khám: bầm tay trái");
            assertThat(health.result()).contains("Đã khám, theo dõi tại phòng");
        });

        FamilyIncidentView general = only(portal.incidents(resident, notAllowed, from(), to()));
        assertThat(general.general()).isEqualTo(seen.general());
        assertThat(general.health()).isEmpty();
    }

    @Test
    void FR_082_processingStatusIsShortened() {
        long open = incident("FALL", "LOW", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        long monitoring = incident("FALL", "LOW", "MONITORING", nurse.accountId(), nurse.accountId());

        var rows = portal.incidents(resident, notAllowed, from(), to());

        assertThat(rows)
                .filteredOn(r -> r.incidentId() == open)
                .singleElement()
                .satisfies(r -> assertThat(r.general().processingStatus()).isEqualTo("HANDLING"));
        assertThat(rows)
                .filteredOn(r -> r.incidentId() == monitoring)
                .singleElement()
                .satisfies(r -> assertThat(r.general().processingStatus()).isEqualTo("MONITORING"));
    }

    @Test
    void Q_72_infectionIncidentGeneralPartOnlyExistenceAndTime() {
        long infection = incident("SUSPECTED_INFECTION", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        FamilyIncidentView row = only(portal.incidents(resident, notAllowed, from(), to()));

        assertThat(row.incidentId()).isEqualTo(infection);
        assertThat(row.general().occurredAt()).isNotNull();
        assertThat(row.general().typeName()).isNull();
        assertThat(row.general().level()).isNull();
        assertThat(row.general().processingStatus()).isNull();
        assertThat(row.health()).isEmpty();
    }

    @Test
    void FR_046a_cancelledIncidentAndOtherResidentsAreExcluded_vitalSignsNeedConsent() {
        incident("FALL", "LOW", "CANCELLED", null, nurse.accountId());

        assertThat(portal.incidents(resident, allowed, from(), to())).isEmpty();
        assertThat(portal.incidents(TestUsers.nextId(), allowed, from(), to())).isEmpty();
        assertThat(portal.vitalSigns(resident, notAllowed, from(), to())).isEmpty();
    }

    private Instant from() {
        return time.now().minus(Duration.ofDays(1));
    }

    private Instant to() {
        return time.now().plus(Duration.ofDays(1));
    }

    private static FamilyIncidentView only(List<FamilyIncidentView> rows) {
        assertThat(rows).hasSize(1);
        return rows.getFirst();
    }
}
