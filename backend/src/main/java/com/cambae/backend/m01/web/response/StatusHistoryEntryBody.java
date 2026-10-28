package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m01.internal.status.service.ResidentStatusService.HistoryEntry;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Một dòng lịch sử trạng thái (contracts/api-001.yaml, schema {@code StatusHistoryEntry}). Lý do, căn cứ, miễn điều
 * kiện có thể là nội dung y tế (chẩn đoán khi chuyển viện, nguyên nhân qua đời) nên thuộc nhóm {@code HEALTH}: người
 * không xem được nhóm này (theo bản đồng ý với người thân, theo vai trò với Hành chính, Kế toán) vẫn thấy trạng thái,
 * lệnh, thời điểm, người thực hiện; ba trường nằm trong {@code hiddenFields} do 002 điền khi tuần tự hóa (BR-M01-08).
 */
@Schema(name = "StatusHistoryEntry")
public record StatusHistoryEntryBody(
        ResidentStatus from,
        ResidentStatus to,
        String command,
        Instant at,
        String performedBy,
        boolean bySystem,
        @FieldGroup(InfoGroup.HEALTH) String reason,
        @FieldGroup(InfoGroup.HEALTH) Map<String, Object> basis,
        @FieldGroup(InfoGroup.HEALTH) Map<String, Object> conditionWaiver,
        List<String> hiddenFields,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    public static StatusHistoryEntryBody of(HistoryEntry e) {
        return new StatusHistoryEntryBody(
                e.from(),
                e.to(),
                e.command(),
                e.at(),
                e.performedBy(),
                e.bySystem(),
                e.reason(),
                e.basis(),
                e.conditionWaiver(),
                List.of(),
                ResidentProfileService.target(e.residentId()));
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
