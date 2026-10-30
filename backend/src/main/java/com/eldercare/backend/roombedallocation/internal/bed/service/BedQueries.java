package com.eldercare.backend.roombedallocation.internal.bed.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.BedStatus;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetPort;
import com.eldercare.backend.roombedallocation.internal.bed.domain.BedDetails;
import com.eldercare.backend.roombedallocation.internal.bed.persistence.BedStore;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T017: đọc bảng giường và quyền của người dùng, không dựng dữ liệu người cao tuổi giả. */
@Service
@Transactional(readOnly = true)
public class BedQueries {
    private final BedStore store;
    private final ObjectProvider<BedAssetPort> assets;
    private final BusinessTime time;
    private final PermissionApi permissions;
    private final CurrentUser user;

    public BedQueries(
            BedStore store,
            ObjectProvider<BedAssetPort> assets,
            BusinessTime time,
            PermissionApi permissions,
            CurrentUser user) {
        this.store = store;
        this.assets = assets;
        this.time = time;
        this.permissions = permissions;
        this.user = user;
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.VIEW)
    public List<BedDetails> list(Long areaId, Long floorId, Long roomId, List<BedStatus> states) {
        return store.list(areaId, floorId, roomId, states).stream()
                .map(this::access)
                .toList();
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.VIEW)
    public BedDetails detail(long id) {
        return access(store.details(id).orElseThrow(NotFoundException::new));
    }

    @RequiresPermission(function = Function.ROOM_BED_CONFIGURATION, operation = Operation.VIEW)
    public java.util.List<com.eldercare.backend.roombedallocation.internal.bed.domain.BedHistoryEntry> history(
            long id, String after) {
        detail(id);
        long cursor = 0;
        if (after != null) {
            try {
                cursor = Long.parseLong(after);
                if (cursor < 0) throw new NumberFormatException();
            } catch (NumberFormatException error) {
                throw new com.eldercare.backend.common.error.BusinessException(
                        org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT,
                        "INVALID_CURSOR",
                        "Khóa trang lịch sử không hợp lệ");
            }
        }
        return store.history(id, cursor);
    }

    private BedDetails access(BedDetails d) {
        var source = assets.getIfAvailable();
        Boolean awaiting = source == null ? null : source.awaitingResidentTransfer(d.id());
        var commands = permissions.allowedCommands(
                user,
                new Target("M03.BED", Long.toString(d.id()), null, d.location().floorId()),
                Map.of("UPDATE", new PermissionKey(Function.ROOM_BED_CONFIGURATION, Operation.UPDATE)));
        return d.withAccess(time.now(), awaiting, commands.stream().sorted().toList());
    }
}
