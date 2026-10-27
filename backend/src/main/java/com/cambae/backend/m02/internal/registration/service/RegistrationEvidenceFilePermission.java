package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.m02.internal.registration.persistence.ResidenceRegistrationStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải tệp bằng chứng khai báo ({@code QuyenTepProvider}; research H-13 của 001, NFR-08, FR-009): chủ sở hữu {@code
 * M02.REGISTRATION_EVIDENCE} là khai báo (bằng chứng nộp, kết quả, khai báo xóa); tải được khi người xem có quyền xem khai
 * báo (Phụ lục 27 dòng "Khai báo tạm trú, lưu trú": Hành chính, Quản lý viện). Khai báo không còn thì không tải được.
 */
@Component
class RegistrationEvidenceFilePermission implements FilePermissionProvider {

    private static final PermissionKey VIEW = new PermissionKey(Function.RESIDENCE_REGISTRATION, Operation.VIEW);

    private final ResidenceRegistrationStore store;
    private final PermissionApi permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    RegistrationEvidenceFilePermission(
            ResidenceRegistrationStore store, PermissionApi permissions, ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return RegistrationNames.EVIDENCE_OWNER_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return store.residentOf(ownerId)
                .map(residentId -> permissions
                        .hasPermission(currentUser.getObject(), VIEW, RegistrationNames.target(ownerId, residentId))
                        .allowed())
                .orElse(false);
    }
}
