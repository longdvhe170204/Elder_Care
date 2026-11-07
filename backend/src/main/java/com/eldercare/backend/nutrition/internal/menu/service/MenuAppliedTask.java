package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.nutrition.internal.schedule.service.M08DailyTask;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Việc hằng ngày: 00:00 thứ Hai thực đơn Công bố → Đã áp dụng (FR-030; research M-15). Xét mọi thực đơn Công bố có tuần đã
 * bắt đầu nên lần chạy bù vẫn chuyển; mỗi thực đơn một transaction, đã chuyển thì bỏ qua.
 */
@Component
class MenuAppliedTask implements M08DailyTask {

    private final MenuService menus;

    MenuAppliedTask(MenuService menus) {
        this.menus = menus;
    }

    @Override
    public String key() {
        return "MENU_APPLIED";
    }

    @Override
    public void run(LocalDate today, Instant now) {
        for (long id : menus.dueForApplied(today)) {
            menus.markApplied(id, now);
        }
    }
}
