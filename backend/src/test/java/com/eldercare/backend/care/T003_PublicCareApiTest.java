package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.*;

/** T003: hợp đồng module phải khả dụng cho consumer và lệnh phải dùng transaction của caller. */
class T003_PublicCareApiTest {
    @Test
    void T003_allContractInterfacesArePublicAndKeepTheirOperations() {
        Map<String, Integer> contracts = Map.ofEntries(
                Map.entry("CarePlanApi", 4),
                Map.entry("TaskTypeApi", 1),
                Map.entry("DayCareAttendanceApi", 6),
                Map.entry("ShiftTaskApi", 6),
                Map.entry("HandoverTaskApi", 3),
                Map.entry("TaskSourceApi", 2),
                Map.entry("AbsenceTaskApi", 1),
                Map.entry("VitalSignTaskApi", 1),
                Map.entry("TaskStatisticsApi", 4),
                Map.entry("TimetableApi", 1),
                Map.entry("PublicCareResultApi", 2));
        contracts.forEach((name, count) -> assertThatCode(() -> {
                    Class<?> api = Class.forName("com.eldercare.backend.care.api." + name);
                    assertThat(api.isInterface()).isTrue();
                    assertThat(Modifier.isPublic(api.getModifiers())).isTrue();
                    assertThat(api.getDeclaredMethods()).hasSize(count);
                })
                .doesNotThrowAnyException());
    }

    @Test
    void T003_moduleCommandsRequireExistingTransaction() throws Exception {
        for (String apiName : new String[] {"ShiftTaskApi", "TaskSourceApi", "AbsenceTaskApi", "VitalSignTaskApi"}) {
            Class<?> api = Class.forName("com.eldercare.backend.care.api." + apiName);
            assertThat(api.getAnnotation(Transactional.class)).isNotNull();
            assertThat(api.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
        }
        for (String method : new String[] {"changeReturnTime", "restoreReturnTime", "checkOut", "recordedInHandover"}) {
            String apiName = method.equals("recordedInHandover") ? "HandoverTaskApi" : "DayCareAttendanceApi";
            var found = java.util.Arrays.stream(Class.forName("com.eldercare.backend.care.api." + apiName)
                            .getDeclaredMethods())
                    .filter(m -> m.getName().equals(method))
                    .findFirst()
                    .orElseThrow();
            assertThat(found.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
        }
    }
}
