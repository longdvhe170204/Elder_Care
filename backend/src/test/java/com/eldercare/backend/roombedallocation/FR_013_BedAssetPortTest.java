package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** G-01, hợp đồng mục 3: mọi lời gọi cổng tài sản giường nằm trong transaction của lệnh gọi. */
class FR_013_BedAssetPortTest {

    @Test
    void FR_013_allAssetPortCallsRequireAnExistingTransaction() {
        for (Method method : BedAssetPort.class.getDeclaredMethods()) {
            Transactional transaction = method.getAnnotation(Transactional.class);
            assertThat(transaction).as("Transaction của %s", method.getName()).isNotNull();
            assertThat(transaction.propagation()).isEqualTo(Propagation.MANDATORY);
        }
    }
}
