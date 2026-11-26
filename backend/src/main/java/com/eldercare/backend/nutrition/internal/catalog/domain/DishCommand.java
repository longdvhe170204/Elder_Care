package com.eldercare.backend.nutrition.internal.catalog.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Lệnh trên món ({@code POST /nutrition/dishes/{id}/{command}}), đều của Nhân viên bếp (Q-445): đưa vào dùng, hủy món Nháp,
 * ngừng hiệu lực (cần lý do).
 */
public enum DishCommand {
    ACTIVATE("activate", false),
    CANCEL("cancel", false),
    DEACTIVATE("deactivate", true);

    private final String path;
    private final boolean requiresReason;

    DishCommand(String path, boolean requiresReason) {
        this.path = path;
        this.requiresReason = requiresReason;
    }

    public static Optional<DishCommand> fromPath(String path) {
        return Arrays.stream(values()).filter(c -> c.path.equals(path)).findFirst();
    }

    public String path() {
        return path;
    }

    public boolean requiresReason() {
        return requiresReason;
    }
}
