package com.cambae.backend.m15.correction.internal.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.approval.api.spi.RequestTypeHandler;
import com.cambae.backend.m15.correction.api.CorrectionPermissions;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CorrectionView;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loại yêu cầu {@code M15.CORRECTION} (000 T058; FR-026 c, nghiệp vụ 1.5): người duyệt là Quản lý viện; áp dụng thì
 * thêm bản đính chính {@code creationMethod = C} trỏ về yêu cầu. Bản gốc đã Hủy ghi nhận hoặc không còn xác nhận lúc áp
 * dụng thì áp dụng không thành (FR-029, FR-038).
 */
@Component
class CorrectionRequestHandler implements RequestTypeHandler {

    static final String TYPE = "M15.CORRECTION";

    /** Nội dung yêu cầu: bản gốc, kiểu và nội dung đúng của bản đính chính đề nghị. */
    record Content(String recordType, long originalRecordId, CorrectionType type, Object correctedContent) {}

    private final CorrectionService corrections;
    private final JsonMapper json;

    CorrectionRequestHandler(CorrectionService corrections, JsonMapper json) {
        this.corrections = corrections;
        this.json = json;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        Content c = read(content);
        corrections.validate(c.recordType(), c.type(), c.correctedContent());
    }

    @Override
    public PermissionKey createPermission() {
        return CorrectionPermissions.REQUEST;
    }

    @Override
    public PermissionKey approvePermission() {
        return CorrectionPermissions.APPROVE;
    }

    @Override
    public Target scope(JsonNode content) {
        Content c = read(content);
        return new Target(
                c.recordType(),
                String.valueOf(c.originalRecordId()),
                null,
                corrections.floorArea(c.recordType(), c.originalRecordId()).orElse(null));
    }

    @Override
    public Optional<Set<Role>> approverRoleRestriction(ApprovalRequestView request) {
        return Optional.of(Set.of(Role.NURSING_HOME_MANAGER));
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        Content c = read(request.content());
        if (!corrections.isConfirmed(c.recordType(), c.originalRecordId())) {
            return List.of(new UnmetCondition("FR-023", "Bản gốc không còn ở trạng thái đã xác nhận"));
        }
        if (corrections.isVoided(c.recordType(), c.originalRecordId())) {
            return List.of(new UnmetCondition("FR-029", "Bản gốc đã Hủy ghi nhận, không đính chính thêm"));
        }
        return List.of();
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        CorrectionView added = corrections.applyApproved(
                read(request.content()), new Reason(request.reason()), request.requesterId(), request.id());
        return new ResultRef(TYPE, String.valueOf(added.id()));
    }

    private Content read(JsonNode content) {
        try {
            Content c = json.treeToValue(content, Content.class);
            if (c == null || c.recordType() == null || c.type() == null) {
                throw new InvalidValueException("Nội dung yêu cầu đính chính thiếu bản gốc hoặc kiểu");
            }
            return c;
        } catch (JacksonException e) {
            throw new InvalidValueException("Nội dung yêu cầu đính chính không hợp lệ");
        }
    }
}
