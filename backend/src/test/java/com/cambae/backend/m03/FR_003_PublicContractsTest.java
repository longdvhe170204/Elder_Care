package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T003: đối chiếu số thao tác và transaction với hợp đồng module 003 mục 1.2–1.9. */
class FR_003_PublicContractsTest {
    @Test
    void FR_018_availabilityUsesResidentGenderRatherThanRoomPolicy() throws Exception {
        String name = "com.cambae.backend.m03.api.dto.ResidentGender";
        assertThat(getClass().getClassLoader().getResource(name.replace('.', '/') + ".class"))
                .isNotNull();
        Class<?> gender = Class.forName(name);
        assertThat(gender.getEnumConstants()).extracting(Object::toString).containsExactly("MALE", "FEMALE");
        assertThat(Class.forName("com.cambae.backend.m03.api.BedStatusApi")
                        .getMethod(
                                "countAvailable",
                                com.cambae.backend.m15.account.api.dto.ScopeFilter.class,
                                com.cambae.backend.m03.api.spi.CareLevelCode.class,
                                gender,
                                com.cambae.backend.m03.api.spi.ResidencyType.class,
                                java.time.Instant.class))
                .isNotNull();
    }

    @Test
    void FR_003_allPublicInterfacesExposeTheContractOperations() throws Exception {
        Map<String, Integer> contracts = Map.of(
                "ResidentLocationApi",
                6,
                "LocationHistoryApi",
                4,
                "BedAllocationCommandApi",
                11,
                "DayCareCapacityApi",
                3,
                "LocationStructureApi",
                8,
                "RoomPolicyApi",
                2,
                "BedStatusApi",
                5,
                "BedStatisticsApi",
                3,
                "BedHandoverAlertApi",
                1);
        for (var contract : contracts.entrySet()) {
            String name = "com.cambae.backend.m03.api." + contract.getKey();
            assertThat(getClass().getClassLoader().getResource(name.replace('.', '/') + ".class"))
                    .as("Hợp đồng %s", name)
                    .isNotNull();
            Class<?> type = Class.forName(name);
            assertThat(type.isInterface()).isTrue();
            assertThat(type.getDeclaredMethods()).hasSize(contract.getValue());
        }
    }

    @Test
    void FR_029_allAllocationCommandsParticipateInTheCallingTransaction() throws Exception {
        String name = "com.cambae.backend.m03.api.BedAllocationCommandApi";
        assertThat(getClass().getClassLoader().getResource(name.replace('.', '/') + ".class"))
                .isNotNull();
        for (var method : Class.forName(name).getDeclaredMethods()) {
            assertThat(method.getAnnotation(Transactional.class))
                    .as(method.getName())
                    .isNotNull();
            assertThat(method.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
        }
    }
}
