package com.eldercare.backend.belongings.internal.returns.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải bản ký phiếu trả ({@code QuyenTepProvider} {@code M12.BAN_KY_TRA}; 013 T007, NFR-08): người xem được đồ gửi
 * của người cao tuổi đó (khóa "Đồ gửi" thao tác Xem, theo phạm vi). {@code ownerId} là bản ghi Trả đầu tiên của lệnh
 * (các đồ trong một lần trả dùng chung một chữ ký, FR-019).
 */
@Component
class ReturnSignaturePermission implements FilePermissionProvider {

    static final String OWNER_TYPE = "M12.RETURN_SIGNATURE";

    private final BelongingStore reads;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> users;

    ReturnSignaturePermission(BelongingStore reads, PermissionApi permissions, ObjectProvider<CurrentUser> users) {
        this.reads = reads;
        this.permissions = permissions;
        this.users = users;
    }

    @Override
    public String ownerType() {
        return OWNER_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return reads.residentOfHandover(ownerId)
                .map(residentId -> permissions
                        .hasPermission(
                                users.getObject(),
                                new PermissionKey(Function.BELONGINGS, Operation.VIEW),
                                new Target("M12.BELONGING_HANDOVER", Long.toString(ownerId), residentId, null))
                        .allowed())
                .orElse(false);
    }
}
