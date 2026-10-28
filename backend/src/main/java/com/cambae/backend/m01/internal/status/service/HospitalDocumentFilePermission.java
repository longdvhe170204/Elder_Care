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
 * Quyền tải giấy tờ bệnh viện (001 T082; FR-047c, BR-M05-32, research H-18): giấy ra viện, đơn thuốc, giấy chuyển viện
 * có chủ sở hữu {@code M01.HOSPITAL_DOCUMENT} là dòng lịch sử trạng thái của lệnh, tải được khi người xem có quyền xem
 * dị ứng, bệnh nền của người cao tuổi đó trong phạm vi (nội dung sức khỏe); Hành chính, Kế toán không tải được.
 */
@Component
class HospitalDocumentFilePermission implements FilePermissionProvider {

    private static final PermissionKey VIEW = new PermissionKey(Function.ALLERGY_CONDITION, Operation.VIEW);

    private final ResidentStatusHistoryRepository histories;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    HospitalDocumentFilePermission(
            ResidentStatusHistoryRepository histories,
            PermissionApi permissions,
            ObjectProvider<CurrentUser> currentUser) {
        this.histories = histories;
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return ResidentStatusService.HOSPITAL_DOCUMENT_OWNER;
    }

    // Không đặt readOnly: kiểm quyền theo phạm vi có thể ghi dấu đã khắc phục lỗi dữ liệu nguồn (002 CHK059)
    @Override
    public boolean canDownload(long ownerId) {
        return histories
                .findById(ownerId)
                .map(history -> permissions
                        .hasPermission(
                                currentUser.getObject(), VIEW, ResidentStatusService.target(history.getResidentId()))
                        .allowed())
                .orElse(false);
    }
}
