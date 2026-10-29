package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.events.LicenseExpiring;
import com.eldercare.backend.administration.account.api.events.PermissionDataError;
import com.eldercare.backend.administration.account.api.events.RecordPendingReview;
import com.eldercare.backend.administration.account.api.events.RecordSyncedAfterPermissionLoss;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 002 T010 (contracts/module-interfaces.md mục 1.10): bốn sự kiện của 002 gửi thông báo qua khuôn {@code common.notification}.
 * Bản ghi sau mất quyền, chờ xem lại: Trưởng tầng của tầng, chưa có thì Người phụ trách ca (cách thay mặc định), không gắn
 * tầng thì Quản lý viện (FR-041a, FR-041c, CHK056). Lỗi dữ liệu quyền, giấy phép sắp hết hạn: Quản lý viện (FR-041b,
 * FR-039). Mức để 009 xếp (009 FR-043b); nội dung chỉ thông tin chung.
 */
class AccountEventNotificationsTest {

    private static final Instant AT = Instant.parse("2026-10-22T03:00:00Z");
    private static final RecipientGroup MANAGERS = new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER);

    private static NotificationRequest only(List<NotificationRequest> requests) {
        assertThat(requests).hasSize(1);
        NotificationRequest r = requests.getFirst();
        assertThat(r.sourceModule()).isEqualTo("M15");
        assertThat(r.level()).isEmpty();
        assertThat(r.occurredAt()).isEqualTo(AT);
        assertThat(r.content()).allSatisfy(part -> assertThat(part.type()).isEqualTo(InformationType.GENERAL));
        assertThat(r.content()).isNotEmpty();
        assertThat(r.shortContent()).isPresent();
        return r;
    }

    @Test
    void FR_041a_syncedAfterPermissionLoss_onFloor_goesToFloorManagerWithDefaultSubstitution() {
        NotificationRequest r =
                only(new RecordSyncedAfterPermissionLoss("M04.CARE_TASK", "77", 2L, AT).notificationRequests());

        assertThat(r.eventType()).isEqualTo("RECORD_SYNCED_AFTER_PERMISSION_LOSS");
        assertThat(r.sourceGroup()).isEqualTo(SourceGroup.OTHER);
        assertThat(r.sourceRecord()).isEqualTo(new SourceRecord("M04.CARE_TASK", "77"));
        assertThat(r.floorAreaId()).contains(2L);
        assertThat(r.recipientGroups())
                .containsExactly(new RecipientGroup.FloorManager(2L, new Substitution.Default()));
    }

    @Test
    void FR_041a_syncedAfterPermissionLoss_withoutFloor_goesToManagers() {
        NotificationRequest r =
                only(new RecordSyncedAfterPermissionLoss("M15.SAMPLE", "5", null, AT).notificationRequests());

        assertThat(r.floorAreaId()).isEmpty();
        assertThat(r.recipientGroups()).containsExactly(MANAGERS);
    }

    @Test
    void FR_041c_pendingReview_followsSameRecipients() {
        NotificationRequest onFloor = only(new RecordPendingReview(9L, 3L, AT).notificationRequests());
        NotificationRequest noFloor = only(new RecordPendingReview(10L, null, AT).notificationRequests());

        assertThat(onFloor.eventType()).isEqualTo("RECORD_PENDING_REVIEW");
        assertThat(onFloor.sourceRecord()).isEqualTo(new SourceRecord("M15.PENDING_REVIEW_RECORD", "9"));
        assertThat(onFloor.recipientGroups())
                .containsExactly(new RecipientGroup.FloorManager(3L, new Substitution.Default()));
        assertThat(noFloor.recipientGroups()).containsExactly(MANAGERS);
    }

    @Test
    void FR_041b_permissionDataError_goesToManagers() {
        NotificationRequest r = only(new PermissionDataError(4L, "M03.BED_ALLOCATION", 12L, "TWO_OPEN_ALLOCATIONS", AT)
                .notificationRequests());

        assertThat(r.eventType()).isEqualTo("PERMISSION_DATA_ERROR");
        assertThat(r.sourceRecord()).isEqualTo(new SourceRecord("M15.PERMISSION_DATA_ERROR", "4"));
        assertThat(r.recipientGroups()).containsExactly(MANAGERS);
        assertThat(r.shortContent().orElseThrow()).contains("M03.BED_ALLOCATION");
    }

    @Test
    void FR_039_licenseExpiring_goesToManagers_withExpiryDate() {
        NotificationRequest r = only(new LicenseExpiring(
                        "PRACTITIONER",
                        31L,
                        LocalDate.of(2026, 11, 20),
                        List.of("INTERNAL_DIAGNOSIS_PRESCRIBING"),
                        List.of(9003L),
                        AT)
                .notificationRequests());

        assertThat(r.eventType()).isEqualTo("LICENSE_EXPIRING");
        assertThat(r.sourceGroup()).isEqualTo(SourceGroup.LICENSE_EXPIRING);
        assertThat(r.sourceRecord()).isEqualTo(new SourceRecord("M15.LICENSE_EXPIRY_WARNING", "PRACTITIONER:31"));
        assertThat(r.eventKey()).isEqualTo("LICENSE_EXPIRING:2026-11-20");
        assertThat(r.recipientGroups()).containsExactly(MANAGERS);
        assertThat(r.shortContent().orElseThrow()).contains("20/11/2026");
    }
}
