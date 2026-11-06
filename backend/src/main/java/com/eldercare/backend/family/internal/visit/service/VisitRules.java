package com.eldercare.backend.family.internal.visit.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.family.internal.visit.domain.VisitSlot;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Tham số của lượt thăm: CFG-M10-04 (khung, sức chứa mỗi khung), CFG-M10-05 (thời hạn đăng ký trước), CFG-M10-06 (số
 * người tối đa mỗi lượt), CFG-M10-07 (nhắc ghi giờ ra). Đọc lúc kiểm tra; đổi CFG-M10-04 chỉ áp cho đăng ký mới vì lượt
 * đã chép khung (FR-032).
 */
@Component
class VisitRules {

    record SlotsConfig(List<SlotConfig> slots, int capacityPerSlot) {}

    record SlotConfig(String from, String to) {}

    record WindowConfig(Amount min, Amount max) {}

    record Amount(long value, String unit) {
        Duration duration() {
            return Duration.of(value, ChronoUnit.valueOf(unit));
        }
    }

    record Slots(List<VisitSlot> slots, int capacity) {}

    private final ParameterApi parameters;

    VisitRules(ParameterApi parameters) {
        this.parameters = parameters;
    }

    Slots slots() {
        SlotsConfig config = parameters.read(ParameterCode.CFG_M10_04).asTable(SlotsConfig.class);
        return new Slots(
                config.slots().stream()
                        .map(s -> new VisitSlot(LocalTime.parse(s.from()), LocalTime.parse(s.to())))
                        .toList(),
                config.capacityPerSlot());
    }

    WindowConfig window() {
        return parameters.read(ParameterCode.CFG_M10_05).asTable(WindowConfig.class);
    }

    int maxVisitors() {
        return parameters.read(ParameterCode.CFG_M10_06).asInt();
    }

    Duration checkOutReminderAfter() {
        return parameters.read(ParameterCode.CFG_M10_07).asDuration();
    }
}
