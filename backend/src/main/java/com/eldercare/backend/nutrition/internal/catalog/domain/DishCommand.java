package com.eldercare.backend.nutrition.internal.catalog.domain;

import java.util.Arrays;
import java.util.Optional;

/**
 * Lệnh trên món ({@code POST /nutrition/dishes/{id}/{command}}): người thực hiện theo bảng trạng thái món (Nhân viên bếp
 * lập, Bác sĩ xác nhận; Q-308) và lệnh cần lý do (trả lại, ngừng hiệu lực).
 */
public enum DishCommand {
    SUBMIT("submit", true, false, false),
    WITHDRAW("withdraw", true, false, false),
    CANCEL("cancel", true, false, false),
    CONFIRM("confirm", false, true, false),
    RETURN("return", false, true, true),
    SUBMIT_REVISION("submit-revision", true, false, false),
    CONFIRM_REVISION("confirm-revision", false, true, false),
    RETURN_REVISION("return-revision", false, true, true),
    WITHDRAW_REVISION("withdraw-revision", true, false, false),
    DEACTIVATE("deactivate", true, true, true);

    private final String path;
    private final boolean byKitchen;
    private final boolean byDoctor;
    private final boolean requiresReason;

    DishCommand(String path, boolean byKitchen, boolean byDoctor, boolean requiresReason) {
        this.path = path;
        this.byKitchen = byKitchen;
        this.byDoctor = byDoctor;
        this.requiresReason = requiresReason;
    }

    public static Optional<DishCommand> fromPath(String path) {
        return Arrays.stream(values()).filter(c -> c.path.equals(path)).findFirst();
    }

    public String path() {
        return path;
    }

    public boolean byKitchen() {
        return byKitchen;
    }

    public boolean byDoctor() {
        return byDoctor;
    }

    public boolean requiresReason() {
        return requiresReason;
    }
}
