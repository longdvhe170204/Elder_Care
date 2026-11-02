package com.eldercare.backend.incident.internal.alert.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/** Danh mục M05 trên bảng chung R-15, không xóa vì có tham chiếu nghiệp vụ. */
public final class IncidentCatalogs {
    /** FR-042: các loại có tác động nghiệp vụ riêng không được Ngừng hiệu lực. */
    public static final java.util.Set<String> PROTECTED_TYPES =
            java.util.Set.of("FALL", "SUSPECTED_INFECTION", "FACILITY", "BELONGINGS_LOST", "BELONGINGS_DAMAGED");

    public static final SharedKind<TrendRuleAttributes> TREND_RULE =
            SharedKind.of("M05.TREND_RULE", TrendRuleAttributes.class).notDeletable();
    public static final SharedKind<IncidentTypeAttributes> INCIDENT_TYPE =
            SharedKind.of("M05.INCIDENT_TYPE", IncidentTypeAttributes.class).notDeletable();
    public static final SharedKind<ProgressionTypeAttributes> PROGRESSION_TYPE = SharedKind.of(
                    "M05.PROGRESSION_TYPE", ProgressionTypeAttributes.class)
            .notDeletable();

    private IncidentCatalogs() {}
}
