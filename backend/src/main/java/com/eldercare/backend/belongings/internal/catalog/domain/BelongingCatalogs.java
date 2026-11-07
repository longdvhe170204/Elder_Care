package com.eldercare.backend.belongings.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * Danh mục của M12 trên {@code catalog_item} (R-15; data-model mục 0.1): loại đồ, mức tình trạng, vị trí lưu giữ; lịch
 * sử dấu "cần đồng ý khi giao sử dụng" ({@code loai_do_lich_su_dau}) ở {@code status_history}.
 */
public final class BelongingCatalogs {

    public static final SharedKind<BelongingTypeAttributes> BELONGING_TYPE =
            SharedKind.of("M12.BELONGING_TYPE", BelongingTypeAttributes.class);
    public static final SharedKind<ConditionLevelAttributes> CONDITION_LEVEL =
            SharedKind.of("M12.CONDITION_LEVEL", ConditionLevelAttributes.class);
    public static final SharedKind<StorageLocationAttributes> STORAGE_LOCATION =
            SharedKind.of("M12.STORAGE_LOCATION", StorageLocationAttributes.class);

    /** FR-001, FR-015a: đổi dấu bắt buộc lý do, lưu lịch sử. */
    public static final HistoryTrack<Void> CONSENT_FLAG_HISTORY =
            new HistoryTrack<>(BELONGING_TYPE.code(), "requiresConsentForUse", Void.class);

    /** Mã của mức cuối "Không dùng được": {@code code_key} giữ đúng một mức cuối (FR-001a). */
    public static final String UNUSABLE_CODE = "UNUSABLE";

    private BelongingCatalogs() {}
}
