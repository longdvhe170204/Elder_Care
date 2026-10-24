package com.cambae.backend.m01.internal.status.service;

import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.m01.internal.status.persistence.ResidentStatusHistoryRepository;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải giấy tờ bằng chứng qua đời (001 T047, T011; FR-047b, research H-13, NFR-08): tệp có chủ sở hữu
 * {@code M01.DEATH_EVIDENCE} là dòng lịch sử trạng thái của lệnh Ghi nhận qua đời, tải được khi người xem có quyền xem
 * "Kết thúc lưu trú, qua đời" của người cao tuổi đó. Người thân không tải được: giấy tờ do cơ sở y tế hoặc cơ quan có
 * thẩm quyền cấp là xác nhận chuyên môn nội bộ, ma trận chỉ cho người thân biết sự việc.
 */
@Component
class DeathEvidenceFilePermission implements FilePermissionProvider {

    private static final PermissionKey VIEW = new PermissionKey(Function.RESIDENCY_END_DEATH, Operation.VIEW);

    private final ResidentStatusHistoryRepository histories;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    DeathEvidenceFilePermission(
            ResidentStatusHistoryRepository histories,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> currentUser) {
        this.histories = histories;
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return ResidentStatusService.DEATH_EVIDENCE_OWNER;
    }

    // Không đặt readOnly: kiểm quyền theo phạm vi có thể ghi dấu đã khắc phục lỗi dữ liệu nguồn (002 CHK059)
    @Override
    public boolean canDownload(long ownerId) {
        CurrentUser user = currentUser.getObject();
        if (user.familyMemberId().isPresent()) {
            return false;
        }
        return histories
                .findById(ownerId)
                .map(history -> permissions
                        .hasPermission(user, VIEW, ResidentStatusService.target(history.getResidentId()))
                        .allowed())
                .orElse(false);
    }
}
