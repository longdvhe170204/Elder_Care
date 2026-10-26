package com.cambae.backend.m02.internal.death.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.internal.death.domain.DeathRecord;
import com.cambae.backend.m02.internal.death.persistence.DeathStore;
import com.cambae.backend.m02.internal.transition.service.ReminderLog;
import com.cambae.backend.m02.internal.transition.service.ReminderRule;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;
    private final BusinessTime time;

    PostDeathReminders(
            DeathStore store,
            PostDeathTaskService tasks,
            ParameterApi parameters,
            ReminderLog log,
            JdbcTemplate jdbc,
            BusinessTime time) {
        this.store = store;
        this.tasks = tasks;
        this.parameters = parameters;
        this.log = log;
        this.jdbc = jdbc;
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
            LocalDate last = lastReminded(record.id());
            if (last == null || !last.plusDays(cycleDays).isAfter(day)) {
                log.send(TYPE, record.id(), day, now);
                sent++;
            }
        }
        return sent;
    }

    private LocalDate lastReminded(long recordId) {
        List<LocalDateTime> found = jdbc.query(
                "SELECT last_reminded_at FROM m02_reminder WHERE type = ? AND subject_id = ?",
                (rs, row) -> rs.getObject(1, LocalDateTime.class),
                TYPE,
                recordId);
        return found.isEmpty() || found.getFirst() == null
                ? null
                : found.getFirst()
                        .toInstant(ZoneOffset.UTC)
                        .atZone(BusinessTime.ZONE)
                        .toLocalDate();
    }
}
