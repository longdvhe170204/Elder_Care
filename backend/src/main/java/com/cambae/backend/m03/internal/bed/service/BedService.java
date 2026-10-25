package com.cambae.backend.m03.internal.bed.service;

import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m03.internal.bed.domain.BedAssetPort;
import com.cambae.backend.m03.internal.bed.domain.BedCreation;
import com.cambae.backend.m03.internal.bed.domain.BedEvent;
import com.cambae.backend.m03.internal.bed.domain.BedTransition;
import com.cambae.backend.m03.internal.bed.domain.BedWarning;
import com.cambae.backend.m03.internal.bed.persistence.BedStore;
import com.cambae.backend.m03.internal.structure.domain.Room;
import com.cambae.backend.m03.internal.structure.domain.RoomType;
import com.cambae.backend.m03.internal.structure.domain.StructureKind;
import com.cambae.backend.m03.internal.structure.persistence.StructureStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T016/Q-238: tạo giường và ghi tăng tài sản trong cùng một transaction do Quản lý viện ra lệnh. */
@Service
public class BedService {
    private final BedStore beds;
    private final StructureStore structures;
    private final BedStateService state;
    private final ObjectProvider<BedAssetPort> assets;
    private final CurrentUser user;
    private final AuditLogApi audit;

    public BedService(
            BedStore beds,
            StructureStore structures,
            BedStateService state,
            ObjectProvider<BedAssetPort> assets,
            CurrentUser user,
            AuditLogApi audit) {
        this.beds = beds;
        this.structures = structures;
        this.state = state;
        this.assets = assets;
        this.user = user;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public BedCreation create(long roomId, String code) {
        if (code == null || code.isBlank() || code.length() > 20)
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_STRUCTURE", "Mã giường không hợp lệ");
        var room = (Room) structures.find(StructureKind.ROOM, roomId, true);
        if (!room.isActive())
            throw new ConditionNotMetException(List.of(new UnmetCondition("FR-004", "Phòng đã ngừng hiệu lực")));
        var type = (RoomType) structures.find(StructureKind.ROOM_TYPE, room.roomTypeId(), false);
        var asset = assets.getIfAvailable();
        if (asset == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa thể ghi tăng tài sản cho giường");
        com.cambae.backend.m03.internal.bed.domain.Bed bed;
        try {
            bed = beds.create(roomId, code);
        } catch (DataIntegrityViolationException error) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_CODE", "Mã giường đã tồn tại trong phòng");
        }
        state.transition(
                bed.id(),
                new BedTransition(
                        BedEvent.CREATE_BED,
                        null,
                        new Basis("Q-238", null, null, null),
                        "BED",
                        bed.id(),
                        null,
                        user.accountId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false));
        asset.recordIncreaseForBed(bed.id());
        beds.refresh(bed);
        var warnings = beds.usableBeds(roomId) > type.standardBeds()
                ? List.of(new BedWarning(
                        "STANDARD_BED_COUNT_EXCEEDED", "Số giường đang sử dụng của phòng vượt số giường tiêu chuẩn"))
                : List.<BedWarning>of();
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_STRUCTURE_CREATED", false))
                .entity("M03.BED", bed.id())
                .before(Map.of())
                .after(Map.of("roomId", roomId, "code", code))
                .details(Map.of("warnings", warnings))
                .build());
        return new BedCreation(bed, warnings);
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public void suspend(long id, com.cambae.backend.common.Reason reason) {
        if (reason == null) throw new com.cambae.backend.common.error.ReasonRequiredException();
        beds.lock(id);
        var source = assets.getIfAvailable();
        if (source == null)
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE, "SOURCE_UNAVAILABLE", "Chưa thể kiểm dấu tài sản giường");
        var failures = new java.util.ArrayList<UnmetCondition>();
        if (beds.hasFutureAllocation(id)) failures.add(new UnmetCondition("FR-011", "Giường có phân bổ tương lai"));
        if (source.awaitingResidentTransfer(id))
            failures.add(new UnmetCondition("BR-M03-15", "Giường đang chờ chuyển người"));
        if (!failures.isEmpty()) throw new ConditionNotMetException(failures);
        changeAvailability(id, BedEvent.SUSPEND, reason);
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public void reuse(long id, com.cambae.backend.common.Reason reason) {
        if (reason == null) throw new com.cambae.backend.common.error.ReasonRequiredException();
        beds.lock(id);
        changeAvailability(id, BedEvent.REUSE, reason);
    }

    private void changeAvailability(long id, BedEvent event, com.cambae.backend.common.Reason reason) {
        state.transition(
                id,
                new BedTransition(
                        event,
                        reason,
                        null,
                        "BED",
                        id,
                        null,
                        user.accountId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false));
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public com.cambae.backend.m03.internal.bed.domain.Bed update(long id, String ifMatch, long roomId, String code) {
        var bed = beds.lock(id);
        com.cambae.backend.common.web.VersionEtag.requireMatch(
                ifMatch,
                bed.version(),
                Map.of("id", id, "roomId", bed.roomId(), "code", bed.code(), "version", bed.version()));
        if (roomId != bed.roomId()) {
            if (structures.bedEverAllocated(id))
                throw new ConditionNotMetException(
                        List.of(new UnmetCondition("FR-007", "Không đổi phòng của giường đã có lịch sử phân bổ")));
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SOURCE_UNAVAILABLE",
                    "Chưa thể đồng bộ vị trí tài sản khi đổi phòng giường");
        }
        if (code == null || code.isBlank() || code.length() > 20)
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_STRUCTURE", "Mã giường không hợp lệ");
        String before = bed.code();
        bed.revise(roomId, code);
        try {
            beds.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_CODE", "Mã giường đã tồn tại trong phòng");
        }
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_STRUCTURE_UPDATED", false))
                .entity("M03.BED", id)
                .before(before.equals(code) ? Map.of() : Map.of("code", before))
                .after(before.equals(code) ? Map.of() : Map.of("code", code))
                .build());
        return bed;
    }
}
