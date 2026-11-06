package com.eldercare.backend.staffing.internal.persistence;

import static com.eldercare.backend.staffing.internal.domain.StaffingRecordData.*;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.Legacy;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Repository;

/** T050: lần import là bản ghi khóa ngay; chỉ ánh xạ id, không thêm nghiệp vụ. */
@Repository
public class StaffingRecordStore {
    private final SimpleRecordApi records;

    public StaffingRecordStore(SimpleRecordApi records) {
        this.records = records;
    }

    public long addImport(String name, int count, long actor) {
        return records.create(
                        IMPORT_BATCH,
                        new NewSimpleRecord<>(
                                null,
                                null,
                                "RECORDED",
                                new ImportBatch(name, count, null),
                                null,
                                null,
                                null,
                                actor,
                                true))
                .id();
    }

    public long importPhysicalId(long logicalId) {
        var row = records.findByUniqueKey(IMPORT_BATCH, "M09.STAFF_IMPORT_BATCH:legacy:" + logicalId)
                .orElseGet(() -> records.require(IMPORT_BATCH, logicalId));
        if (Legacy.logicalId(row.id(), row.content().legacy(), "staff_import_batch") != logicalId)
            throw new NotFoundException();
        return row.id();
    }

    public Long importLogicalId(Long physicalId) {
        if (physicalId == null) return null;
        var row = records.require(IMPORT_BATCH, physicalId);
        return Legacy.logicalId(row.id(), row.content().legacy(), "staff_import_batch");
    }

    public List<SimpleRecord<ImportBatch>> imports() {
        return records.listByStatus(IMPORT_BATCH, Set.of("RECORDED"));
    }
}
