package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.incident.api.dto.FamilyIncidentGeneralPart;
import com.eldercare.backend.incident.api.dto.FamilyIncidentHealthPart;
import com.eldercare.backend.incident.api.dto.FamilyIncidentView;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import java.util.List;
import java.util.Optional;

/**
 * Danh sách trường phần chung và phần sức khỏe của sự cố cho người thân, giữ ở một chỗ (FR-082, Q-352, Q-358; research
 * S-19a). Phần chung: loại hiện hành, thời điểm, mức, trạng thái rút gọn; sự cố lây nhiễm chỉ còn "có sự cố" và thời
 * điểm (Q-72). Phần sức khỏe chỉ khi bên gọi cho biết bản đồng ý bao gồm người thân đó.
 */
public final class FamilyIncidentPartitioner {
    static final String SUSPECTED_INFECTION = "SUSPECTED_INFECTION";

    private FamilyIncidentPartitioner() {}

    public static FamilyIncidentView partition(
            IncidentView incident,
            IncidentTypeView currentType,
            List<String> progressions,
            Optional<String> transfer,
            boolean includeHealth) {
        var at = incident.occurredAt() != null ? incident.occurredAt() : incident.discoveredAt();
        var general = SUSPECTED_INFECTION.equals(currentType.code())
                ? new FamilyIncidentGeneralPart(null, at, null, null)
                : new FamilyIncidentGeneralPart(
                        currentType.name(), at, incident.level().name(), processingStatus(incident.status()));
        Optional<FamilyIncidentHealthPart> health = includeHealth
                ? Optional.of(new FamilyIncidentHealthPart(
                        incident.description(),
                        incident.initialResponse(),
                        progressions,
                        Optional.ofNullable(incident.resolution()),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Optional.empty(),
                        transfer))
                : Optional.empty();
        return new FamilyIncidentView(incident.id(), general, health);
    }

    /** "Viện đang xử lý / đang theo dõi / đã xử lý xong" (S-19a). */
    static String processingStatus(String status) {
        return switch (status) {
            case "MONITORING" -> "MONITORING";
            case "CLOSED" -> "RESOLVED";
            default -> "HANDLING";
        };
    }
}
