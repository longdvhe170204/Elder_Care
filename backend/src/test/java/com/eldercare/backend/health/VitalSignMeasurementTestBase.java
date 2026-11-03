package com.eldercare.backend.health;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Cô lập quyền khi kiểm nghiệp vụ MySQL; kiểm từ chối ở FR_011_MeasurementValidationTest. */
public abstract class VitalSignMeasurementTestBase extends MySqlIntegrationTestBase {
    @MockitoBean
    protected PermissionApi measurementPermissions;

    @MockitoBean
    protected CurrentUser measurementActor;

    @BeforeEach
    void allowMeasurementFixture() {
        when(measurementPermissions.hasPermission(any(), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(measurementPermissions.hasPermissionAt(any(), any(), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
    }

    protected void asMeasurementUser(CurrentUser user) {
        when(measurementActor.accountId()).thenReturn(user.accountId());
        when(measurementActor.roles()).thenReturn(user.roles());
        when(measurementActor.isScheduler()).thenReturn(false);
    }
}
