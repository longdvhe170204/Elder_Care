package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.ResidentModuleStateApi;
import com.eldercare.backend.health.api.events.MissingPersonalThresholdReminder;
import com.eldercare.backend.health.internal.governance.domain.ResidentVitalSignState;
import com.eldercare.backend.health.internal.governance.service.VitalSignLock;
import com.eldercare.backend.health.internal.threshold.persistence.ThresholdVersionStore;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.ResidentStatusApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusPeriod;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Người cao tuổi chưa có ngưỡng cá nhân (FR-025, BR-M06-02, research S-07): Đang lưu trú quá CFG-M06-01 kể từ Hoàn
 * tất tiếp nhận (lần đầu vào Đang lưu trú của hồ sơ) mà không có phiên bản cá nhân Hiệu lực hay Chờ hiệu lực. Nhắc mọi
 * Bác sĩ một lần ({@code missingThresholdRemindedAt}); danh sách tính lúc đọc.
 */
@Service
public class MissingThresholdReminders {
    private static final Set<ResidentStatus> IN_RESIDENCE = Set.of(ResidentStatus.IN_RESIDENCE);

    private final ResidentApi residents;
    private final ResidentStatusApi statuses;
    private final ThresholdVersionStore store;
    private final ResidentModuleStateApi states;
    private final VitalSignLock lock;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;

    public MissingThresholdReminders(
            ResidentApi residents,
            ResidentStatusApi statuses,
            ThresholdVersionStore store,
            ResidentModuleStateApi states,
            VitalSignLock lock,
            ParameterApi parameters,
            ApplicationEventPublisher events) {
        this.residents = residents;
        this.statuses = statuses;
        this.store = store;
        this.states = states;
        this.lock = lock;
        this.parameters = parameters;
        this.events = events;
    }

    /** Danh sách "chưa có ngưỡng cá nhân" tại thời điểm {@code now}. */
    @Transactional(readOnly = true)
    public List<Long> missing(Instant now) {
        var out = new ArrayList<Long>();
        var wait = parameters.read(ParameterCode.CFG_M06_01).asDuration();
        for (long residentId : residents.byStatus(IN_RESIDENCE)) {
            var admitted = admittedAt(residentId, now);
            if (admitted.isEmpty() || admitted.get().plus(wait).isAfter(now)) continue;
            if (!store.hasPersonalOpen(residentId)) out.add(residentId);
        }
        return out;
    }

    /** Việc hằng ngày: nhắc các Bác sĩ một lần cho mỗi người vừa vào danh sách. */
    @Transactional
    public int remind(Instant now) {
        int reminded = 0;
        for (long residentId : missing(now)) {
            if (alreadyReminded(states.get(ResidentVitalSignState.KIND, residentId))) continue;
            lock.lock(residentId);
            if (alreadyReminded(states.lockForUpdate(ResidentVitalSignState.KIND, residentId))) continue;
            states.save(ResidentVitalSignState.KIND, residentId, new ResidentVitalSignState(now));
            events.publishEvent(new MissingPersonalThresholdReminder(
                    residentId, "M06.MISSING_PERSONAL_THRESHOLD:" + residentId + ":" + now.toEpochMilli(), now));
            reminded++;
        }
        return reminded;
    }

    /** Q-330: ngưỡng cá nhân bị Ngừng mà không còn phiên bản nào thì các Bác sĩ được nhắc lại một lần. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void allowReminderAgain(long residentId) {
        if (store.hasPersonalOpen(residentId)) return;
        lock.lock(residentId);
        states.save(ResidentVitalSignState.KIND, residentId, new ResidentVitalSignState(null));
    }

    private Optional<Instant> admittedAt(long residentId, Instant now) {
        return statuses.statusPeriods(residentId, IN_RESIDENCE, Instant.EPOCH, now).stream()
                .map(StatusPeriod::from)
                .min(Comparator.naturalOrder());
    }

    private static boolean alreadyReminded(Optional<ResidentVitalSignState> state) {
        return state.map(s -> s.missingThresholdRemindedAt() != null).orElse(false);
    }
}
