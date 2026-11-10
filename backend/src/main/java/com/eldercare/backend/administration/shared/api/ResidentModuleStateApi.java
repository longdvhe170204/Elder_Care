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

    /**
     * Dòng riêng của loại, khóa {@code (resident_id, kind.code())} thay vì {@code (resident_id, module)} (bổ sung
     * 2026-11-10 cho 014 {@code M04.HOAT_DONG_NCT}, quyết định 09/11 (6)): module đã có dòng chung do phần khác ghi dùng dòng
     * riêng để không ghi chung một JSON. Không đọc, không đè dòng chung của module.
     */
    <T> Optional<T> getOwnRow(SharedKind<T> kind, long residentId);

    /** Như {@link #getOwnRow}, khóa dòng tới hết transaction. */
    <T> Optional<T> lockOwnRowForUpdate(SharedKind<T> kind, long residentId);

    /** Thêm hoặc thay dòng riêng của loại; gọi sau {@link #lockOwnRowForUpdate} khi cần tuần tự. */
    <T> void saveOwnRow(SharedKind<T> kind, long residentId, T data);
}
