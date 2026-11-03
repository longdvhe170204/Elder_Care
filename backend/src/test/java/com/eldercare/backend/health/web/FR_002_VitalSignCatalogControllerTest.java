package com.eldercare.backend.health.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.health.internal.catalog.domain.*;
import com.eldercare.backend.health.internal.catalog.service.VitalSignCatalogService;
import com.eldercare.backend.health.web.request.ReasonBody;
import com.eldercare.backend.health.web.request.VitalSignCatalogRequest;
import com.eldercare.backend.health.web.request.VitalSignCatalogRequest.ComponentRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class FR_002_VitalSignCatalogControllerTest {

    @Test
    void FR_002_mapsEnglishHttpContractWithoutExposingVersion() {
        var service = mock(VitalSignCatalogService.class);
        var range = JsonMapper.builder()
                .build()
                .createObjectNode()
                .putObject("VALUE")
                .put("min", 1)
                .put("max", 2);
        when(service.list())
                .thenReturn(List.of(new VitalSignCatalogView(
                        7,
                        "HEART_RATE",
                        "Nhịp tim",
                        "bpm",
                        true,
                        List.of(new VitalSignComponentDefinition("VALUE", "Giá trị", 0)),
                        range,
                        false,
                        true,
                        3)));

        var result = new VitalSignCatalogController(service).list();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.code()).isEqualTo("HEART_RATE");
            assertThat(item.components())
                    .singleElement()
                    .extracting("decimalPlaces")
                    .isEqualTo(0);
            assertThat(item.validRange()).isEqualTo(range);
            assertThat(item.active()).isTrue();
        });
    }

    @Test
    void FR_002_forwardsCreateUpdateAndDeactivateCommands() {
        var service = mock(VitalSignCatalogService.class);
        var controller = new VitalSignCatalogController(service);
        var request = new VitalSignCatalogRequest(
                null,
                "HEART_RATE",
                "Nhịp tim",
                "bpm",
                true,
                List.of(new ComponentRequest("VALUE", "Giá trị", 0)),
                null);
        var view = new VitalSignCatalogView(
                7,
                "HEART_RATE",
                "Nhịp tim",
                "bpm",
                true,
                List.of(new VitalSignComponentDefinition("VALUE", "Giá trị", 0)),
                null,
                true,
                true,
                3);
        when(service.create(request.toInput())).thenReturn(view);
        when(service.update(7, "\"3\"", request.toInput())).thenReturn(view);

        assertThat(controller.create(null, request).getStatusCode().value()).isEqualTo(201);
        assertThat(controller.update(7, "\"3\"", null, request).getStatusCode().value())
                .isEqualTo(200);
        controller.deactivate(7, null, new ReasonBody(Reason.of("Không còn dùng")));

        verify(service).create(request.toInput());
        verify(service).update(7, "\"3\"", request.toInput());
        verify(service).deactivate(7, Reason.of("Không còn dùng"));
    }
}
