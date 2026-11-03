package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.care.web.DayCareAttendanceCorrectionController;
import com.eldercare.backend.care.web.request.AttendanceCorrectionWrite;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import org.junit.jupiter.api.Test;

class FR_015_AttendanceCorrectionControllerTest {
    @Test
    void FR_015_unsupportedVoidMustNotCreateAnApprovalRequest() {
        var api = mock(CorrectionApi.class);
        when(api.create(anyString(), anyLong(), any(), any(), any()))
                .thenReturn(new CorrectionResult.PendingApproval(null));
        var controller = new DayCareAttendanceCorrectionController(api);
        assertThatThrownBy(() -> controller.correct(
                        7,
                        "test-void",
                        new AttendanceCorrectionWrite(CorrectionType.VOID, null, new Reason("Bản ghi nhầm"))))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(api);
    }
}
