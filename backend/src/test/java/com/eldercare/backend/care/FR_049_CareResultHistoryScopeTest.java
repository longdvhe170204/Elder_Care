package com.eldercare.backend.care;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.care.web.CareResultController;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class FR_049_CareResultHistoryScopeTest {
    @Test
    void FR_049_historyWithoutDateBoundsNeverPassesNullToLocationSource() throws Exception {
        var constructor = CareResultController.class.getConstructors()[0];
        var dependencies = new HashMap<Class<?>, Object>();
        var arguments = Arrays.stream(constructor.getParameterTypes())
                .map(type -> {
                    Object dependency = mock(type);
                    if (dependency instanceof BusinessTime time)
                        when(time.now()).thenReturn(Instant.parse("2026-11-01T03:00:00Z"));
                    dependencies.put(type, dependency);
                    return dependency;
                })
                .toArray();
        var controller = (CareResultController) constructor.newInstance(arguments);
        var beds = (BedAllocationApi) dependencies.get(BedAllocationApi.class);
        controller.history(77, null, null, null);
        verify(beds).openAt(eq(77L), notNull());
        controller.history(77, Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-02T00:00:00Z"), null);
        verify(beds, times(2)).openAt(77L, Instant.parse("2026-11-01T03:00:00Z"));
    }
}
