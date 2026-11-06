package com.eldercare.backend.roombedallocation.internal.asset.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import com.eldercare.backend.roombedallocation.internal.asset.persistence.AssetStore;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 019 T018 lượt 2 (module-interfaces mục 3; FR-013, Q-314): loại yêu cầu {@code M03.ASSET_DISPOSAL}. Phụ trách bộ phận
 * quản lý tài sản lập (lệnh {@code request-disposal} kiểm), Quản lý viện duyệt, không tự duyệt; áp dụng thì tài sản Đã
 * thanh lý. Nội dung {@code {assetId}}; lý do là lý do của yêu cầu. Thanh lý giường (FR-015, Q-51) ở Phase 8.
 */
@Component
class AssetDisposalHandler implements RequestTypeHandler {
    static final String TYPE = "M03.ASSET_DISPOSAL";

    record Content(Long assetId) {}

    private final AssetStore store;
    private final AssetService assets;
    private final JsonMapper json;

    AssetDisposalHandler(AssetStore store, @Lazy AssetService assets, JsonMapper json) {
        this.store = store;
        this.assets = assets;
        this.json = json;
    }

    @Override
    public String typeCode() {
        return TYPE;
    }

    @Override
    public void validateContent(JsonNode content) {
        var asset = store.find(read(content).assetId())
                .orElseThrow(() -> new InvalidValueException("assetId không phải tài sản"));
    }

    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.ASSET_VEHICLE, Operation.CREATE);
    }

    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.ASSET_VEHICLE, Operation.APPROVE);
    }

    @Override
    public Target scope(JsonNode content) {
        long id = read(content).assetId();
        return new Target(
                AssetKinds.ENTITY,
                Long.toString(id),
                null,
                store.find(id).map(a -> a.floorAreaId()).orElse(null));
    }

    @Override
    public Optional<Set<Role>> approverRoleRestriction(ApprovalRequestView request) {
        return Optional.of(Set.of(Role.NURSING_HOME_MANAGER));
    }

    @Override
    public boolean allowSelfApproval() {
        return false;
    }

    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        boolean disposable = store.find(read(request.content()).assetId())
                .filter(a -> a.status() != AssetStatus.DISPOSED)
                .isPresent();
        return disposable ? List.of() : List.of(new UnmetCondition("FR-013", "Tài sản đã thanh lý hoặc không còn"));
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        long id = read(request.content()).assetId();
        assets.dispose(id, request.id());
        return new ResultRef(AssetKinds.ENTITY, Long.toString(id));
    }

    private Content read(JsonNode content) {
        try {
            var c = json.treeToValue(content, Content.class);
            if (c == null || c.assetId() == null) throw new InvalidValueException("Nội dung cần assetId");
            return c;
        } catch (JacksonException e) {
            throw new InvalidValueException("Nội dung yêu cầu thanh lý không hợp lệ");
        }
    }
}
