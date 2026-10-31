package com.eldercare.backend.care.internal.catalog.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/** Loại nội bộ M04; module khác đọc qua API chăm sóc. */
public final class CareCatalogKinds {
    public static final SharedKind<TaskTypeAttributes> TASK_TYPES =
            SharedKind.of("M04.TASK_TYPE", TaskTypeAttributes.class).notDeletable();
    public static final SharedKind<ResultValueAttributes> RESULT_VALUES =
            SharedKind.of("M04.RESULT_VALUE", ResultValueAttributes.class).notDeletable();

    public static long publicId(com.eldercare.backend.administration.shared.api.dto.CatalogItem<?> row) {
        LegacyCatalogId legacy = switch (row.attributes()) {
            case TaskTypeAttributes a -> a.legacy();
            case ResultValueAttributes a -> a.legacy();
            default -> null;
        };
        return legacy == null ? row.id() : legacy.id();
    }

    public static final SharedKind<ReasonAttributes> REASONS = SharedKind.of(
                    "M04.NON_PERFORMANCE_REASON_TYPE", ReasonAttributes.class)
            .notDeletable();

    private CareCatalogKinds() {}
}
