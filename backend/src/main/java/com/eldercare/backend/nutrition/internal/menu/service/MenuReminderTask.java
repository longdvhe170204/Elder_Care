package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.nutrition.api.events.MenuPublicationReminder;
import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuStatus;
import com.eldercare.backend.nutrition.internal.schedule.service.M08DailyTask;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobMarkers;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Việc hằng ngày: từ CFG-M08-07 ngày trước thứ Hai kế tiếp mà tuần đó chưa có thực đơn Công bố, nhắc một lần mỗi tuần
 * (FR-027, BR-M08-16). Lần chạy bù trong khoảng tới trước thứ Hai vẫn nhắc; dấu {@code M08.JOB.MENU_REMINDER} theo tuần.
 */
@Component
class MenuReminderTask implements M08DailyTask {

    private static final String KEY = "MENU_REMINDER";

    private final MenuService menus;
    private final M08JobMarkers markers;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;

    MenuReminderTask(
            MenuService menus, M08JobMarkers markers, ParameterApi parameters, ApplicationEventPublisher events) {
        this.menus = menus;
        this.markers = markers;
        this.parameters = parameters;
        this.events = events;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    @Transactional
    public void run(LocalDate today, Instant now) {
        LocalDate week = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        long lead = parameters.read(ParameterCode.CFG_M08_07).asDuration().toDays();
        if (today.isBefore(week.minusDays(lead))) {
            return;
        }
        Optional<MenuStatus> status = menus.ofWeek(week).map(Menu::status);
        if (status.map(MenuStatus::published).orElse(false)) {
            return;
        }
        if (markers.firstTime(KEY, "M08.MENU_WEEK", week.toEpochDay(), week.toString())) {
            events.publishEvent(new MenuPublicationReminder(
                    week,
                    status.filter(s -> s == MenuStatus.PENDING_PUBLICATION).isPresent(),
                    now));
        }
    }
}
