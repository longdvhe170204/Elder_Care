package com.eldercare.backend.admission.internal.deposit.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.admission.api.events.DepositStatusChanged;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** 004 T040 (FR-032): sự kiện đổi trạng thái đặt cọc của 017 dẫn tới kiểm lại điều kiện đúng hợp đồng. */
class DepositStatusListenerTest {

    @Test
    void FR_032_depositStatusChanged_recheckTheSameContract() {
        DepositRecheckService recheck = mock(DepositRecheckService.class);
        DepositStatusListener listener = new DepositStatusListener(recheck);

        listener.onChanged(new DepositStatusChanged(
                41L, 7L, DepositStatus.NOT_SATISFIED, 99L, 5L, Instant.parse("2026-11-09T03:00:00Z")));

        verify(recheck).recheck(41L);
    }
}
