package com.eldercare.backend.incident.internal.critical.service;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thông báo của nguy kịch (FR-050b → FR-050d, research S-13, S-19). Nguồn là cảnh báo nguy kịch để 009 dừng nhắc gọi
 * theo nguồn. Người thân chỉ nhận nội dung tối thiểu như FR-047, không có nguyện vọng; mã Q-354 theo plan điểm báo lại
 * 10: người liên hệ chính {@code PRIMARY_CONTACT_EMERGENCY}, người đại diện {@code WISH_RECONFIRMATION}.
 */
final class CriticalCareNotices {
    static final String SOURCE_TYPE = "M05.ALERT";
    private static final String FAMILY_CONTENT =
            "Có tình huống khẩn cấp liên quan người thân của ông/bà tại viện. Đề nghị liên hệ viện.";

    private CriticalCareNotices() {}

    /** FR-050b (b), (c): đội ngũ và người thân, mức Khẩn cấp. */
    static List<NotificationRequest> flagRecorded(
            long alertId, long residentId, Long floorAreaId, boolean noOnDutyDoctor, long actorId, Instant at) {
        var team = new ArrayList<RecipientGroup>();
        team.add(doctors(noOnDutyDoctor));
        team.add(new RecipientGroup.AssignedNurse(residentId, new Substitution.Default()));
        if (floorAreaId != null) team.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()));
        team.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        String key = key(alertId, "CRITICAL_FLAG_RECORDED");
        return List.of(
                request(
                        alertId,
                        "CRITICAL_FLAG_RECORDED",
                        key,
                        NotificationLevel.URGENT,
                        residentId,
                        floorAreaId,
                        team,
                        new ContentPart(
                                InformationType.HEALTH,
                                "Người cao tuổi nguy kịch: cần thực hiện nguyện vọng cuối đời. Mở cảnh báo để xem"
                                        + " nguyện vọng và xử lý."),
                        "Nguy kịch",
                        actorId,
                        at,
                        Map.of("alertId", alertId),
                        Optional.empty(),
                        Optional.empty()),
                family(
                        alertId,
                        key + ":PRIMARY_CONTACT",
                        residentId,
                        floorAreaId,
                        actorId,
                        at,
                        new RecipientGroup.PrimaryContact(residentId),
                        Q354Case.PRIMARY_CONTACT_EMERGENCY),
                family(
                        alertId,
                        key + ":REPRESENTATIVE",
                        residentId,
                        floorAreaId,
                        actorId,
                        at,
                        new RecipientGroup.Representative(residentId),
                        Q354Case.WISH_RECONFIRMATION));
    }

    /**
     * FR-050b (d): Bác sĩ trực (không có thì mọi Bác sĩ, FR-047b) và Điều dưỡng phụ trách gọi xác nhận lại, theo thứ tự
     * gọi bắt đầu từ người đại diện; không tạo yêu cầu gọi điện (Q-307).
     */
    static List<NotificationRequest> reconfirmationRequested(
            long alertId,
            long residentId,
            Long floorAreaId,
            boolean noOnDutyDoctor,
            List<Long> callOrder,
            long actorId,
            Instant at) {
        return List.of(request(
                alertId,
                "WISH_RECONFIRMATION_REQUESTED",
                key(alertId, "WISH_RECONFIRMATION_REQUESTED"),
                NotificationLevel.URGENT,
                residentId,
                floorAreaId,
                List.of(
                        doctors(noOnDutyDoctor),
                        new RecipientGroup.AssignedNurse(residentId, new Substitution.Default())),
                new ContentPart(
                        InformationType.HEALTH,
                        "Cần gọi xác nhận lại nguyện vọng cuối đời, bắt đầu từ người đại diện, rồi ghi kết quả."),
                "Xác nhận lại nguyện vọng",
                actorId,
                at,
                Map.of("alertId", alertId, "callOrder", List.copyOf(callOrder)),
                Optional.empty(),
                Optional.empty()));
    }

    /** FR-050a: dấu tạm, mọi Bác sĩ đang hoạt động được báo để xác nhận hoặc gỡ (Q-218), mức Trung bình. */
    static List<NotificationRequest> provisionalConfirmation(
            long flagId, long alertId, long residentId, Long floorAreaId, long actorId, Instant at) {
        return List.of(request(
                alertId,
                "PROVISIONAL_CRITICAL_FLAG",
                key(alertId, "PROVISIONAL_CRITICAL_FLAG:" + flagId),
                NotificationLevel.MEDIUM,
                residentId,
                floorAreaId,
                List.of(new RecipientGroup.FacilityRole(Role.DOCTOR)),
                new ContentPart(
                        InformationType.HEALTH,
                        "Điều dưỡng đã ghi dấu nguy kịch tạm. Cần Bác sĩ xác nhận hoặc gỡ dấu."),
                "Dấu nguy kịch tạm",
                actorId,
                at,
                Map.of("alertId", alertId, "flagId", flagId),
                Optional.empty(),
                Optional.empty()));
    }

    /** FR-050d "đưa về nhà": Hành chính, Trưởng tầng thực hiện Cho tạm vắng qua quy trình đón (Q-218, Q-307), Trung bình. */
    static List<NotificationRequest> homeLeave(
            long alertId, long residentId, Long floorAreaId, long actorId, Instant at) {
        var groups = new ArrayList<RecipientGroup>();
        groups.add(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF));
        if (floorAreaId != null) groups.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()));
        return List.of(request(
                alertId,
                "TEMPORARY_HOME_LEAVE_REQUESTED",
                key(alertId, "TEMPORARY_HOME_LEAVE_REQUESTED"),
                NotificationLevel.MEDIUM,
                residentId,
                floorAreaId,
                groups,
                new ContentPart(
                        InformationType.GENERAL,
                        "Cần thực hiện Cho tạm vắng lý do \"về nhà theo nguyện vọng cuối đời\" qua quy trình đón."),
                "Về nhà theo nguyện vọng",
                actorId,
                at,
                Map.of("alertId", alertId),
                Optional.empty(),
                Optional.empty()));
    }

    static String key(long alertId, String suffix) {
        return SOURCE_TYPE + ":" + alertId + ":" + suffix;
    }

    private static RecipientGroup doctors(boolean noOnDutyDoctor) {
        return noOnDutyDoctor
                ? new RecipientGroup.FacilityRole(Role.DOCTOR)
                : new RecipientGroup.OnDutyDoctor(new Substitution.Default());
    }

    private static NotificationRequest family(
            long alertId,
            String key,
            long residentId,
            Long floorAreaId,
            long actorId,
            Instant at,
            RecipientGroup group,
            Q354Case q354Case) {
        return request(
                alertId,
                "CRITICAL_FLAG_RECORDED",
                key,
                NotificationLevel.URGENT,
                residentId,
                floorAreaId,
                List.of(group),
                new ContentPart(InformationType.GENERAL, FAMILY_CONTENT),
                "Khẩn cấp",
                actorId,
                at,
                Map.of("alertId", alertId),
                Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.of(q354Case));
    }

    private static NotificationRequest request(
            long alertId,
            String eventType,
            String key,
            NotificationLevel level,
            long residentId,
            Long floorAreaId,
            List<RecipientGroup> groups,
            ContentPart content,
            String shortContent,
            long actorId,
            Instant at,
            Map<String, Object> params,
            Optional<InformationSubject> subject,
            Optional<Q354Case> q354Case) {
        return new NotificationRequest(
                "M05",
                eventType,
                SourceGroup.HEALTH_ALERT,
                new SourceRecord(SOURCE_TYPE, Long.toString(alertId)),
                key,
                Optional.of(level),
                Optional.of(residentId),
                Optional.ofNullable(floorAreaId),
                groups,
                List.of(content),
                Optional.of(shortContent),
                Optional.of("Cảnh báo"),
                Optional.of(actorId),
                false,
                at,
                params,
                false,
                subject,
                q354Case);
    }
}
