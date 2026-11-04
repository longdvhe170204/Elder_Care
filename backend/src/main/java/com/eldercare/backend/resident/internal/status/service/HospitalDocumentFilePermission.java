package com.eldercare.backend.resident.internal.status.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.resident.internal.status.persistence.ResidentStatusHistoryStore;
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

    private final ResidentStatusHistoryStore histories;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    HospitalDocumentFilePermission(
            ResidentStatusHistoryStore histories, PermissionApi permissions, ObjectProvider<CurrentUser> currentUser) {
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
