package com.eldercare.backend.admission.internal.death.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.admission.internal.death.domain.DeathRecord;
import com.eldercare.backend.admission.internal.death.persistence.DeathStore;
import com.eldercare.backend.admission.internal.persistence.M02RunStore;
import com.eldercare.backend.admission.internal.transition.service.ReminderLog;
import com.eldercare.backend.admission.internal.transition.service.ReminderRule;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Nhắc Hành chính khi danh sách việc sau qua đời còn mục Chưa hoàn thành (FR-072; loại nhắc {@code POST_DEATH_TASKS}, mức
 * Nhẹ), theo chu kỳ CFG-M02-09 (mặc định 1 ngày), tới khi mọi mục xong. Mỗi lần nhắc tính lại danh sách từ module sở hữu
 * trước (research T-13) nên danh sách xong giữa hai lần nhắc thì đóng hồ sơ và không nhắc nữa. Nhắc không tạo bản ghi nghiệp
 * vụ nào (000 FR-010a).
 */
@Component
class PostDeathReminders implements ReminderRule {

    static final String TYPE = "POST_DEATH_TASKS";

    private final DeathStore store;
    private final PostDeathTaskService tasks;
    private final ParameterApi parameters;
    private final ReminderLog log;
    private final M02RunStore runs;
    private final BusinessTime time;

    PostDeathReminders(
            DeathStore store,
            PostDeathTaskService tasks,
            ParameterApi parameters,
            ReminderLog log,
            M02RunStore runs,
            BusinessTime time) {
        this.store = store;
        this.tasks = tasks;
        this.parameters = parameters;
        this.log = log;
        this.runs = runs;
        this.time = time;
    }

    @Override
    public int remind(LocalDate day) {
        long cycleDays = Math.max(
                1, parameters.read(ParameterCode.CFG_M02_09).asDuration().toDays());
        Instant now = time.now();
        int sent = 0;
        for (DeathRecord record : store.open()) {
            if (tasks.recalculate(record).closedAt() != null) {
                continue;
            }
            LocalDate last = runs.lastReminded(TYPE, record.id()).orElse(null);
            if (last == null || !last.plusDays(cycleDays).isAfter(day)) {
                log.send(TYPE, record.id(), day, now);
                sent++;
            }
        }
        return sent;
    }
}
