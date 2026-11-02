package com.eldercare.backend.administration.shared;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/** Loại mẫu chỉ dùng trong test của bảng dùng chung (module M15). */
public final class SharedTestKinds {

    public record Attributes(@NotBlank String colour, int size) {}

    public static final SharedKind<Attributes> CATALOG = SharedKind.of("M15.TEST_CATALOG", Attributes.class);
    public static final SharedKind<Attributes> OTHER_CATALOG =
            SharedKind.of("M15.TEST_OTHER_CATALOG", Attributes.class);
    public static final SharedKind<Void> FIXED_CATALOG =
            SharedKind.of("M15.TEST_FIXED_CATALOG", Void.class).notDeletable();
    public static final SharedKind<Attributes> EVENT = SharedKind.of("M15.TEST_EVENT", Attributes.class);
    public static final SharedKind<Attributes> OTHER_EVENT = SharedKind.of("M15.TEST_OTHER_EVENT", Attributes.class);
    public static final SharedKind<Attributes> RECORD = SharedKind.of("M15.TEST_RECORD", Attributes.class);
    public static final SharedKind<Attributes> OTHER_RECORD = SharedKind.of("M15.TEST_OTHER_RECORD", Attributes.class);
    public static final SharedKind<Attributes> STATE = SharedKind.of("M15.TEST_STATE", Attributes.class);
    public static final SharedKind<Attributes> OTHER_MODULE_STATE = SharedKind.of("M14.TEST_STATE", Attributes.class);
    public static final HistoryTrack<Attributes> TRACK =
            new HistoryTrack<>("M15.TEST_ENTITY", "status", Attributes.class);
    public static final HistoryTrack<Void> OTHER_TRACK = new HistoryTrack<>("M15.TEST_ENTITY", "owner", Void.class);

    /** Tên, mã, khóa không trùng giữa các lần chạy (schema test giữ dữ liệu). */
    public static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private SharedTestKinds() {}
}
