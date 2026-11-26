package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.spi.LicenseApi;
import com.eldercare.backend.administration.account.api.spi.LicenseValidity;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.dose.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.dose.service.DoseCommandPermission;
import com.eldercare.backend.medication.internal.dose.service.DoseExtrasService;
import com.eldercare.backend.medication.internal.dose.service.DoseReviewService;
import com.eldercare.backend.medication.internal.dose.service.DoseViewService;
import com.eldercare.backend.medication.internal.dose.service.DoseWorkflowService;
import com.eldercare.backend.medication.web.DoseController;
import com.eldercare.backend.medication.web.request.DoseRequests.DoseResultRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * BR-M07-21, Q-61 (review BF-07 2026-11-25): lệnh liều qua HTTP (Cho dùng, Từ chối, Không thực hiện, Hoãn, Nhận liều,
 * Bỏ nhận) chỉ cho người có quyền chuyên môn Phát thuốc (Phụ lục 27: Điều dưỡng) và giấy phép còn hiệu lực, như đường
 * ngoại tuyến ({@code DoseResultOfflineHandler}); trước đây mọi tài khoản đã đăng nhập đều gọi được.
 */
class DoseCommandPermissionTest {

    private static final String KEY = "1-2026-11-25T01:00:00Z";
    private static final PermissionKey ADMINISTER =
            new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.PROFESSIONAL);

    private final DoseAdministrationService administration = mock(DoseAdministrationService.class);
    private final DoseWorkflowService workflow = mock(DoseWorkflowService.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ProfessionalActivityApi activities = mock(ProfessionalActivityApi.class);
    private final LicenseApi licenses = mock(LicenseApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final BusinessTime time =
            new BusinessTime(Clock.fixed(Instant.parse("2026-11-25T03:00:00Z"), ZoneOffset.UTC));
    private final DoseController controller = new DoseController(
            administration,
            mock(DoseReviewService.class),
            mock(DoseViewService.class),
            mock(DoseExtrasService.class),
            workflow,
            user,
            time,
            new DoseCommandPermission(permissions, activities, licenses, time));

    private List<Executable> commands() {
        return List.of(
                () -> controller.administer(KEY, null, null),
                () -> controller.refuse(KEY, null, null),
                () -> controller.skip(KEY, null, null),
                () -> controller.postpone(KEY, null, null),
                () -> controller.hold(KEY, null, null));
    }

    private void staff(Set<PermissionKey> granted) {
        when(user.accountId()).thenReturn(7L);
        when(user.staffId()).thenReturn(Optional.of(70L));
        when(permissions.permissions(user)).thenReturn(granted);
        when(activities.requiredLicenseType(ProfessionalActivityApi.MEDICATION_ADMINISTRATION))
                .thenReturn(Optional.of("NURSING_LICENSE"));
    }

    @Test
    void BR_M07_21_withoutAdministrationPermission_everyDoseCommandIsForbidden() {
        staff(Set.of(new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.VIEW)));
        for (Executable command : commands()) {
            PermissionDeniedException denied = catchThrowableOfType(PermissionDeniedException.class, command::execute);
            assertThat(denied).isNotNull();
            assertThat(denied.errorCode()).isEqualTo("PERMISSION_DENIED");
        }
        verifyNoInteractions(administration, workflow);
    }

    @Test
    void Q_61_withoutValidLicense_everyDoseCommandIsForbiddenAsLegal() {
        staff(Set.of(ADMINISTER));
        when(licenses.valid(anyLong(), anyString(), any())).thenReturn(Optional.empty());
        for (Executable command : commands()) {
            PermissionDeniedException denied = catchThrowableOfType(PermissionDeniedException.class, command::execute);
            assertThat(denied).isNotNull();
            assertThat(denied.errorCode()).isEqualTo("LEGAL_CONDITION_NOT_MET");
        }
        verifyNoInteractions(administration, workflow);
    }

    @Test
    void nurseWithValidLicense_holdReachesTheService() {
        staff(Set.of(ADMINISTER));
        when(licenses.valid(eq(70L), eq("NURSING_LICENSE"), any()))
                .thenReturn(Optional.of(new LicenseValidity(1L, "Giấy phép", null, Set.of())));
        DoseResultRequest request = mock(DoseResultRequest.class);
        // Bản giả DoseViewService không dựng phản hồi; chỉ kiểm lệnh không bị chặn và tới được service
        assertThat(catchThrowable(() -> controller.hold(KEY, null, request)))
                .isNotInstanceOf(PermissionDeniedException.class);
        verify(workflow).hold(eq(KEY), any(), eq(7L), any());
    }
}
