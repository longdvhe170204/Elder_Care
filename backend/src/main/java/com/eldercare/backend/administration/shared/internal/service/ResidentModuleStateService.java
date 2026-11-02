package com.eldercare.backend.administration.shared.internal.service;

import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.internal.persistence.ResidentModuleStateStore;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link ResidentModuleStateApi} trên {@code resident_module_state} (research R-15). */
@Service
class ResidentModuleStateService implements ResidentModuleStateApi {

    private final ResidentModuleStateStore store;
    private final SharedContent content;
    private final BusinessTime time;

    ResidentModuleStateService(ResidentModuleStateStore store, SharedContent content, BusinessTime time) {
        this.store = store;
        this.content = content;
        this.time = time;
    }

    @Override
    public <T> Optional<T> get(SharedKind<T> kind, long residentId) {
        return store.find(kind.module(), residentId, false).map(json -> content.read(kind.content(), json));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> Optional<T> lockForUpdate(SharedKind<T> kind, long residentId) {
        return store.find(kind.module(), residentId, true).map(json -> content.read(kind.content(), json));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> void save(SharedKind<T> kind, long residentId, T data) {
        store.save(kind.module(), residentId, content.write(data), time.now());
    }
}
