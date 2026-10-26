package com.cambae.backend.m02.internal.death.service;

import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.m02.internal.death.persistence.DeathStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Quyền tải bằng chứng qua đời (loại chủ sở hữu {@code M02.DEATH_EVIDENCE}; FR-069, NFR-08): người đã tải bằng chứng lên và
 * ghi nhận lệnh được xem lại tệp của mình, kể cả Hành chính không xem được nguyên nhân (BR-M02-21). Người khác xem qua quyền
 * xem bản ghi qua đời ở màn của bản ghi, không tải thẳng tệp.
 */
@Component
class DeathEvidenceFilePermission implements FilePermissionProvider {

    private final DeathStore store;
    private final ObjectProvider<CurrentUser> currentUser;

    DeathEvidenceFilePermission(DeathStore store, ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.currentUser = currentUser;
    }

    @Override
    public String ownerType() {
        return DeathTransition.EVIDENCE_OWNER_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return store.find(ownerId)
                .map(record -> record.recordedBy() == currentUser.getObject().accountId())
                .orElse(false);
    }
}
