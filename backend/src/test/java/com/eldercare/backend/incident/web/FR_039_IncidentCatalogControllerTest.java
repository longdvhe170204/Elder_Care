package com.eldercare.backend.incident.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.incident.internal.alert.domain.*;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.web.request.IncidentTypeRequest;
import com.eldercare.backend.incident.web.request.ProgressionTypeRequest;
import com.eldercare.backend.incident.web.request.TrendRuleRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class FR_039_IncidentCatalogControllerTest {

    @Test
    void FR_039_mapsEnglishReadContractsWithoutExposingVersion() {
        var service = mock(IncidentCatalogService.class);
        when(service.trendRules())
                .thenReturn(List.of(
                        new TrendRuleView("WEIGHT_LOSS", "Sụt cân", true, IncidentLevel.MEDIUM, "CFG-M05-01", 2)));
        when(service.incidentTypes())
                .thenReturn(
                        List.of(new IncidentTypeView(8, "FALL", "Ngã", IncidentLevel.CRITICAL, false, false, true, 1)));
        when(service.progressionTypes())
                .thenReturn(List.of(
                        new ProgressionTypeView(9, "CPR", "Hồi sức tim phổi", ProgressionGroup.ACTION, true, 0)));

        var controller = new IncidentCatalogController(service);

        assertThat(controller.trendRules()).singleElement().extracting("code").isEqualTo("WEIGHT_LOSS");
        assertThat(controller.incidentTypes())
                .singleElement()
                .extracting("defaultLevel")
                .isEqualTo(IncidentLevel.CRITICAL);
        assertThat(controller.progressionTypes())
                .singleElement()
                .extracting("resuscitation")
                .isEqualTo(true);
    }

    @Test
    void FR_039_forwardsWriteCommandsAndPreservesOptionalActive() {
        var service = mock(IncidentCatalogService.class);
        var controller = new IncidentCatalogController(service);
        var trend = new TrendRuleRequest(false, IncidentLevel.CRITICAL, Reason.of("Tăng rủi ro"));
        var incident = new IncidentTypeRequest("CUSTOM", "Khác", IncidentLevel.LOW, true, false);
        var progression = new ProgressionTypeRequest("OBSERVE", "Theo dõi", ProgressionGroup.MONITORING, false);
        when(service.updateTrendRule("WEIGHT_LOSS", "\"2\"", trend.toInput()))
                .thenReturn(
                        new TrendRuleView("WEIGHT_LOSS", "Sụt cân", false, IncidentLevel.CRITICAL, "CFG-M05-01", 3));
        when(service.createIncidentType(incident.toInput()))
                .thenReturn(new IncidentTypeView(8, "CUSTOM", "Khác", IncidentLevel.LOW, true, false, true, 0));
        when(service.updateIncidentType(8, "\"1\"", incident.toInput(), false))
                .thenReturn(new IncidentTypeView(8, "CUSTOM", "Khác", IncidentLevel.LOW, true, false, false, 2));
        when(service.createProgressionType(progression.toInput()))
                .thenReturn(new ProgressionTypeView(9, "OBSERVE", "Theo dõi", ProgressionGroup.MONITORING, false, 0));

        controller.updateTrendRule("WEIGHT_LOSS", "\"2\"", trend);
        controller.createIncidentType(incident);
        controller.updateIncidentType(8, "\"1\"", incident);
        controller.createProgressionType(progression);

        verify(service).updateTrendRule("WEIGHT_LOSS", "\"2\"", trend.toInput());
        verify(service).createIncidentType(incident.toInput());
        verify(service).updateIncidentType(8, "\"1\"", incident.toInput(), false);
        verify(service).createProgressionType(progression.toInput());
    }
}
