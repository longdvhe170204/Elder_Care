package com.eldercare.backend.care.internal.catalog.service;

import static com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds.TASK_TYPES;

import com.eldercare.backend.care.api.TaskTypeApi;
import com.eldercare.backend.care.api.dto.TaskRequirements;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chứng chỉ được đọc mỗi lần,không lưu cache làm chậm hiệu lực thay đổi FR-038. */
@Service
@Transactional(readOnly = true)
public class CareTaskTypeLookup implements TaskTypeApi {
    private final CareCatalogStore store;

    public CareTaskTypeLookup(CareCatalogStore store) {
        this.store = store;
    }

    public TaskRequirements requirements(long id) {
        var data = store.require(TASK_TYPES, id).attributes();
        return new TaskRequirements(data.defaultRole(), data.certificates());
    }
}
