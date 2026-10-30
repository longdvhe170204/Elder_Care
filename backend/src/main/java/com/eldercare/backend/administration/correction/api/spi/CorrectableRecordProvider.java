package com.eldercare.backend.administration.correction.api.spi;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import java.util.Optional;

/**
 * SPI do module sở hữu một loại bản ghi nhóm 3 cài (contracts/module-interfaces.md mục 1.6). Loại không có provider thì
 * không có đính chính (chi phí đã chốt, giao dịch số dư): 409 {@code CORRECTION_NOT_SUPPORTED}.
 */
public interface CorrectableRecordProvider {

    /** Mã loại bản ghi, ví dụ {@code M07.MEDICATION_DOSE}. */
    String recordType();

    /** Bản gốc tồn tại và đã xác nhận (US2 #8). */
    boolean isConfirmed(long id);

    /** Tài khoản đã ghi bản gốc (FR-026 a). */
    long originalRecorder(long id);

    /** Tầng/khu vực của bản gốc; rỗng là loại không gắn tầng (FR-026 c, CHK051). */
    Optional<Long> floorArea(long id);

    /**
     * Người cao tuổi của bản gốc, để dòng nhật ký đính chính tra được theo người cao tuổi (FR-049); rỗng là loại không gắn
     * người cao tuổi.
     */
    default Optional<Long> residentId(long id) {
        return Optional.empty();
    }

    /** Người dùng có quyền ghi nhận loại bản ghi này (FR-026 a, c). */
    boolean canRecord(CurrentUser user);

    /** Kiểm nội dung đúng; sai thì ném {@code InvalidValueException}. */
    void validateContent(Object correctedContent);

    /** Nội dung bản gốc để hiển thị chuỗi đính chính (contracts/api-000.yaml {@code originalContent}). */
    Object originalContent(long id);

    /** Module thu hẹp thêm người được đính chính (FR-026 "module MAY thu hẹp"). */
    default boolean allows(CurrentUser user, CreationMethod method, long id) {
        return true;
    }

    /** Tác động dây chuyền do module (ví dụ 010 lập khoản điều chỉnh), chạy trong transaction của lệnh. */
    default void afterCorrection(CorrectionView correction) {}
}
