package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import org.junit.jupiter.api.Test;

class TypeLockTest {

    @Test
    void generatesStandardTypeLocks() {
        assertThat(TypeLock.forVitalSign(12L)).isEqualTo("VUOT_NGUONG:12");
        assertThat(TypeLock.forTrend("SUT_CAN")).isEqualTo("XU_HUONG:SUT_CAN");
        assertThat(TypeLock.forMissedMeasurement(34L)).isEqualTo("BO_LO_LAN_DO:34");
        assertThat(TypeLock.forOverdueTask(56L)).isEqualTo("QUA_HAN:56");
        assertThat(TypeLock.forMedication("ALLERGY", 78L)).isEqualTo("THUOC:ALLERGY:78");
        assertThat(TypeLock.forManual(90L)).isEqualTo("THU_CONG:90");
        assertThat(TypeLock.forCritical(11L)).isEqualTo("NGUY_KICH:11");
    }
}
