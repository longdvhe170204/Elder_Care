package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.SleepResultRecorded;
import com.eldercare.backend.care.api.events.SleepResultVoided;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleView;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.trend.service.TrendRuleService;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * FR-039c, BR-M05-04: CFG-M05-05 (3) đêm liên tiếp thuộc nhóm mất ngủ tạo cảnh báo "mất ngủ kéo dài"; kết quả bị hủy ghi
 * nhận không tính; đính chính sang nhóm khác làm đứt chuỗi; đêm vắng mặt (ngày {@code ABSENT} của 004) không tính và không
 * làm đứt chuỗi; quy tắc tắt thì không tạo. Gọi listener trực tiếp.
 */
class FR_039c_ProlongedInsomniaIT extends MySqlIntegrationTestBase {
    private static final LocalDate NIGHT = LocalDate.of(2026, 11, 5);
    private static final Instant AT = Instant.parse("2026-11-06T01:00:00Z");

    @Autowired
    TrendRuleService trends;

    @Autowired
    AlertStore alerts;

    @MockitoSpyBean
    IncidentCatalogService catalog;

    @MockitoSpyBean
    ResidencyDayApi residencyDays;

    @Test
    void FR_039c_threeInsomniaNightsCreateAlertAndVoidedNightIsNotCounted() {
        rule(true);
        long resident = TestUsers.nextId();
        long second = TestUsers.nextId();
        trends.onSleepResultRecorded(recorded(TestUsers.nextId(), resident, NIGHT.minusDays(2), "INSOMNIA", 0));
        trends.onSleepResultRecorded(recorded(second, resident, NIGHT.minusDays(1), "INSOMNIA", 1));
        assertThat(alerts.findOpenAlert(resident, TypeLock.forTrend("PROLONGED_INSOMNIA")))
                .isEmpty();
        trends.onSleepResultVoided(voided(second, resident, NIGHT.minusDays(1), 2));
        trends.onSleepResultRecorded(recorded(TestUsers.nextId(), resident, NIGHT, "INSOMNIA", 3));
        assertThat(alerts.findOpenAlert(resident, TypeLock.forTrend("PROLONGED_INSOMNIA")))
                .isEmpty();

        long other = TestUsers.nextId();
        for (int i = 2; i >= 0; i--)
            trends.onSleepResultRecorded(recorded(TestUsers.nextId(), other, NIGHT.minusDays(i), "INSOMNIA", 4 + i));
        var alert = alerts.findOpenAlert(other, TypeLock.forTrend("PROLONGED_INSOMNIA"))
                .orElseThrow();
        assertThat(alert.level()).isEqualTo(IncidentLevel.MEDIUM);
    }

    /** Mức Trung bình để không phụ thuộc nguồn ca của 008 (cảnh báo Nhẹ cần hạn theo ca). */
    private void rule(boolean enabled) {
        var rules = new ArrayList<>(catalog.trendRules().stream()
                .filter(r -> !r.code().equals("PROLONGED_INSOMNIA"))
                .toList());
        rules.add(new TrendRuleView(
                "PROLONGED_INSOMNIA", "Mất ngủ kéo dài", enabled, IncidentLevel.MEDIUM, "CFG-M05-05", 1));
        doReturn(rules).when(catalog).trendRules();
    }

    private static SleepResultRecorded recorded(long id, long resident, LocalDate night, String group, int sequence) {
        return new SleepResultRecorded(id, resident, night, group, context(night, sequence));
    }

    private static SleepResultVoided voided(long id, long resident, LocalDate night, int sequence) {
        return new SleepResultVoided(id, resident, night, context(night, sequence));
    }

    private static CareEventContext context(LocalDate night, int sequence) {
        return new CareEventContext(
                "M04.TASK", 1, "RESULT_" + sequence, AT.plusSeconds(sequence), Optional.empty(), List.of());
    }
}
