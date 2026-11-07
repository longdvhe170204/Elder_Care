package com.eldercare.backend.belongings.internal.incident.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommand.IncidentChoice;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.incident.api.IncidentApi;
import com.eldercare.backend.incident.api.IncidentCommandApi;
import com.eldercare.backend.incident.api.dto.IncidentView;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Cổng sang 007 cho lệnh đồ gửi ({@code SuCoDoGuiService}; 013 T021; FR-021, FR-024, FR-025, BR-M12-02, Q-307, research
 * K-07): ghi sự cố mới ({@code IncidentCommandApi.record}, MANDATORY: lỗi thì cả lệnh rollback), kiểm sự cố có sẵn
 * ({@code IncidentApi.view}), ghi diễn biến "đã tìm thấy". Hệ thống không tự tạo sự cố: chỉ làm theo lựa chọn của người
 * ghi lệnh. Production chưa có bean của 007 thì 503 {@code SOURCE_UNAVAILABLE} (plan mục "Kế hoạch triển khai R-15"
 * điểm 14).
 */
@Component
public class BelongingIncidents {

    static final String SOURCE = "BELONGINGS";
    static final String LOST_TYPE = "BELONGINGS_LOST";
    static final String DAMAGED_TYPE = "BELONGINGS_DAMAGED";
    static final String FOUND_PROGRESSION = "FOUND";

    private static final Set<String> LEVELS = Set.of("LOW", "MEDIUM", "CRITICAL");

    private final ObjectProvider<IncidentCommandApi> commands;
    private final ObjectProvider<IncidentApi> incidents;
    private final IncidentTypeLookup types;

    BelongingIncidents(
            ObjectProvider<IncidentCommandApi> commands,
            ObjectProvider<IncidentApi> incidents,
            IncidentTypeLookup types) {
        this.commands = commands;
        this.incidents = incidents;
        this.types = types;
    }

    /**
     * Thực hiện lựa chọn sự cố của một đồ cho lần Báo thất lạc hoặc Ghi hư hỏng {@code kind}; trả mã sự cố cần lưu ở bản
     * ghi, hoặc {@code null} khi người ghi không chọn sự cố. Gọi trong transaction của lệnh.
     */
    public Long choose(
            IncidentChoice choice,
            HandoverType kind,
            long residentId,
            Instant occurredAt,
            String reason,
            CurrentUser user) {
        if (choice == null || choice.method() == null || "NONE".equals(choice.method())) {
            return null;
        }
        if (kind != HandoverType.REPORT_LOST && kind != HandoverType.RECORD_DAMAGE) {
            throw invalid("Chỉ Báo thất lạc, Ghi hư hỏng mới ghi hoặc liên kết sự cố (FR-021)");
        }
        return switch (choice.method()) {
            case "NEW" -> record(choice, kind, residentId, occurredAt, reason, user);
            case "LINK" -> {
                if (choice.incidentId() == null) {
                    throw invalid("Liên kết sự cố cần mã sự cố");
                }
                validateLink(choice.incidentId(), residentId);
                yield choice.incidentId();
            }
            default -> throw invalid("Cách ghi sự cố không hợp lệ: " + choice.method());
        };
    }

    /** Sự cố có sẵn phải tồn tại, của cùng người cao tuổi, chưa Đã hủy (FR-021). */
    public void validateLink(long incidentId, long residentId) {
        IncidentApi source = incidents.getIfAvailable();
        if (source == null) {
            throw unavailable();
        }
        IncidentView view = source.view(incidentId).orElse(null);
        if (view == null || !Objects.equals(view.residentId(), residentId) || "VOIDED".equals(view.status())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_INCIDENT",
                    "Sự cố không tồn tại, không cùng người cao tuổi hoặc đã hủy");
        }
    }

    /**
     * Sự cố còn mở (Mới, Đang xử lý, Đang theo dõi) cho điều kiện (b) kết thúc lưu trú (FR-025); chưa có bản thật của 007
     * hoặc không tìm thấy sự cố thì coi như không mở (điều kiện (d) do 007 xét).
     */
    public boolean isOpen(long incidentId) {
        IncidentApi source = incidents.getIfAvailable();
        return source != null
                && source.view(incidentId)
                        .map(v -> Set.of("NEW", "IN_PROGRESS", "MONITORING").contains(v.status()))
                        .orElse(false);
    }

    /** Tìm thấy: thêm diễn biến vào sự cố liên kết, không đóng sự cố (FR-025). */
    public void found(long incidentId, String content, CurrentUser user) {
        IncidentCommandApi source = commands.getIfAvailable();
        if (source == null) {
            throw unavailable();
        }
        source.addProgression(incidentId, FOUND_PROGRESSION, content, user);
    }

    private long record(
            IncidentChoice choice,
            HandoverType kind,
            long residentId,
            Instant occurredAt,
            String reason,
            CurrentUser user) {
        if (choice.level() == null || !LEVELS.contains(choice.level())) {
            throw invalid("Ghi sự cố mới cần chọn mức LOW, MEDIUM hoặc CRITICAL (FR-023)");
        }
        IncidentCommandApi source = commands.getIfAvailable();
        if (source == null) {
            throw unavailable();
        }
        String description =
                choice.description() == null || choice.description().isBlank() ? reason : choice.description();
        long typeId = types.idOf(kind == HandoverType.REPORT_LOST ? LOST_TYPE : DAMAGED_TYPE);
        return source.record(
                new RecordIncidentCommand(
                        residentId,
                        typeId,
                        SOURCE,
                        occurredAt,
                        occurredAt,
                        null,
                        choice.place(),
                        null,
                        description,
                        choice.level(),
                        null,
                        null,
                        false),
                user);
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", message);
    }

    private static BusinessException unavailable() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa có nguồn sự cố của feature 007");
    }
}
