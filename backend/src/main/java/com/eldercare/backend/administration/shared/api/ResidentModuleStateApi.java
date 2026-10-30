package com.eldercare.backend.administration.shared.api;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.util.Optional;

/**
 * Trạng thái theo người cao tuổi của từng module {@code resident_module_state} (khóa {@code (resident_id, module)},
 * module lấy từ {@code kind}; module-interfaces 000 mục 1.8). Lệnh ghi chạy trong transaction của lệnh gọi.
 */
public interface ResidentModuleStateApi {

    <T> Optional<T> get(SharedKind<T> kind, long residentId);

    /** Như {@link #get}, khóa dòng tới hết transaction ({@code SELECT … FOR UPDATE}). */
    <T> Optional<T> lockForUpdate(SharedKind<T> kind, long residentId);

    /** Thêm hoặc thay dữ liệu của module cho người cao tuổi; gọi sau {@link #lockForUpdate} khi cần tuần tự. */
    <T> void save(SharedKind<T> kind, long residentId, T data);
}
