package com.eldercare.backend.care.internal.activity.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;

/**
 * 014 T003 (R-15, data-model mục 0.1): loại ở bảng chung, không tạo bảng riêng. Hoạt động đặt ở {@code simple_record} thay
 * {@code catalog_item} vì {@code catalog_item} buộc tên Hiệu lực duy nhất theo loại, chặn hai tầng cùng tên hoạt động (plan
 * 014, Phase 2).
 */
public final class ActivityKinds {
    public static final SharedKind<ActivityTypeAttributes> ACTIVITY_TYPES =
            SharedKind.of("M04.ACTIVITY_TYPE", ActivityTypeAttributes.class).notDeletable();
    public static final SharedKind<Void> INTEREST_GROUPS =
            SharedKind.of("M04.INTEREST_GROUP", Void.class).notDeletable();
    public static final SharedKind<Activity> ACTIVITIES = SharedKind.of("M04.ACTIVITY", Activity.class);
    public static final SharedKind<TemplateVersion> TEMPLATE_VERSIONS =
            SharedKind.of("M04.TEMPLATE_VERSION", TemplateVersion.class);
    public static final SharedKind<RegularParticipant> REGULAR_PARTICIPANTS =
            SharedKind.of("M04.REGULAR_PARTICIPANT", RegularParticipant.class);

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";
    public static final String ENDED = "ENDED";

    private ActivityKinds() {}
}
