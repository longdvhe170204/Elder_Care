package com.eldercare.backend.administration.shared.api;

import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import java.util.Collection;

/**
 * Xóa dòng bảng dùng chung của bản ghi hết hạn lưu giữ, bằng tài khoản {@code eldercare_retention} trong transaction của
 * chuỗi bản ghi (research R-15 mục 2, R-11; module-interfaces 000 mục 1.8). Chỉ {@code administration.retention} gọi.
 */
public interface SharedRowRetention {

    /** Loại đối tượng hồ sơ người cao tuổi: xóa thêm dòng theo {@code resident_id}. */
    String RESIDENT = "M01.RESIDENT";

    /**
     * Dòng {@code status_history}, {@code record_event}, {@code simple_record}, {@code reminder_marker} có đối tượng là
     * một trong {@code records}; đối tượng {@link #RESIDENT} thì thêm {@link #deleteForResidents}.
     */
    void deleteFor(Collection<EntityRef> records);

    /** Dòng {@code simple_record}, {@code resident_module_state} của các người cao tuổi. */
    void deleteForResidents(Collection<Long> residentIds);
}
