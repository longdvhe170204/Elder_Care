package com.eldercare.backend.administration.shared.api.spi;

import java.util.Map;

/**
 * Loại bản ghi cũ của module sau gộp bảng (research R-15 mục 5, module-interfaces 000 mục 1.8): đính chính và nhật ký
 * ghi trước khi gộp giữ loại cũ (chỉ thêm, không sửa dòng), nên đọc theo loại mới phải gồm cả loại cũ. Bản ghi gộp giữ
 * nguyên id, nên chỉ ánh xạ loại. Module cài khi viết migration gộp bảng {@code V<NNN>_95}.
 */
public interface LegacyRecordTypeSource {

    /** Loại cũ → loại mới, ví dụ {@code M01.CARE_LEVEL_HISTORY} → {@code M01.STATUS_HISTORY}. */
    Map<String, String> legacyRecordTypes();
}
