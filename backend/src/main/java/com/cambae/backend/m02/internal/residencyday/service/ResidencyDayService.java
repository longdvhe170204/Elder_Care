package com.cambae.backend.m02.internal.residencyday.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.ResidencyDayApi;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.dto.DayCareSessionView;
import com.cambae.backend.m02.api.dto.ResidencyDayView;
import com.cambae.backend.m02.internal.absence.domain.Absence;
import com.cambae.backend.m02.internal.absence.domain.AbsenceDay;
import com.cambae.backend.m02.internal.absence.persistence.AbsenceStore;
import com.cambae.backend.m02.internal.absence.service.DayCareSessions;
import com.cambae.backend.m02.internal.application.persistence.AdmissionApplicationStore;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.ContractVersion;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.death.persistence.DeathStore;
import com.cambae.backend.m02.internal.ending.domain.ResidencyEnding;
import com.cambae.backend.m02.internal.ending.persistence.ResidencyEndingStore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dữ liệu mọi ngày lưu trú cho 010 (tên logic {@code NgayLuuTruApi}; FR-052, FR-060, FR-066, FR-070, FR-074, FR-075a). Chỉ
 * đọc: mỗi ngày nội trú thuộc một hợp đồng đã ký, mang ngày vắng đã tính ở {@code absence_day}, các dấu "chưa vào ở", "quá hạn
 * hợp đồng", "chờ quyết định", "không có dòng chính sách" và dấu ngoài thời gian tính phí (sau mốc dừng). Bán trú tính theo
 * buổi ở {@link DayCareSessions}.
 */
@Service
class ResidencyDayService implements ResidencyDayApi {

    private static final Set<ContractStatus> SIGNED =
            Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED);

    /** Ngày vắng kèm lượt vắng chứa nó. */
    private record AbsentDay(long absenceId, AbsenceDay day) {}

    private final ContractStore contracts;
    private final AbsenceStore absences;
    private final AdmissionApplicationStore applications;
    private final ResidencyEndingStore endings;
    private final DeathStore deaths;
    private final DayCareSessions dayCare;
    private final BusinessTime time;

    ResidencyDayService(
            ContractStore contracts,
            AbsenceStore absences,
            AdmissionApplicationStore applications,
            ResidencyEndingStore endings,
            DeathStore deaths,
            DayCareSessions dayCare,
            BusinessTime time) {
        this.contracts = contracts;
        this.absences = absences;
        this.applications = applications;
        this.endings = endings;
        this.deaths = deaths;
        this.dayCare = dayCare;
        this.time = time;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResidencyDayView> daysBetween(long residentId, LocalDate from, LocalDate to) {
        List<Contract> signed = contracts.ofResident(residentId).stream()
                .filter(c -> SIGNED.contains(c.status()))
                .toList();
        Optional<LocalDate> stop = billingStopDate(residentId);
        Map<LocalDate, AbsentDay> absent = absentDays(residentId);
        Optional<LocalDate> admittedOn = applications.admittedOn(residentId);
        boolean admissionOpen = applications
                .findLatest(residentId)
                .map(a -> a.status().isOpen())
                .orElse(false);
        LocalDate today = time.today();

        List<ResidencyDayView> days = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            Optional<Contract> covering = covering(signed, day);
            if (covering.isEmpty()) {
                continue;
            }
            Contract contract = covering.get();
            Optional<ContractVersion> version = contracts.versionAt(contract.id(), day);
            if (version.isEmpty() || isDayCare(version.get())) {
                continue;
            }
            AbsentDay away = absent.get(day);
            boolean notYetAdmitted =
                    admittedOn.isPresent() ? day.isBefore(admittedOn.get()) : admissionOpen && day.isBefore(today);
            days.add(new ResidencyDayView(
                    day,
                    contract.id(),
                    version.get().id(),
                    away == null ? "PRESENT" : "ABSENT",
                    away == null ? null : away.absenceId(),
                    away == null ? null : away.day().sequenceNo(),
                    away == null ? null : away.day().coefficient().intValue(),
                    away != null && away.day().pendingDecision(),
                    away != null && away.day().noPolicyLine(),
                    notYetAdmitted,
                    contract.status() == ContractStatus.ACTIVE
                            && contract.overdueSince() != null
                            && !day.isBefore(contract.overdueSince()),
                    stop.isPresent() && day.isAfter(stop.get())));
        }
        return days;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DayCareSessionView> dayCareSessions(long residentId, LocalDate from, LocalDate to) {
        Optional<LocalDate> stop = billingStopDate(residentId);
        LocalDate last = stop.isPresent() && stop.get().isBefore(to) ? stop.get() : to;
        return dayCare.between(residentId, from, last);
    }

    /**
     * Mốc dừng tính phí: ngày qua đời (FR-070), ngày Kết thúc lưu trú thực tế (FR-066), ngày kết thúc dự kiến của hồ sơ Đang
     * chuẩn bị, hoặc ngày hợp đồng Chấm dứt gần nhất (Hủy tiếp nhận, FR-075a); theo thứ tự đó.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> billingStopDate(long residentId) {
        Optional<LocalDate> died = deaths.ofResident(residentId).map(r -> dayOf(r.deathAt()));
        if (died.isPresent()) {
            return died;
        }
        Optional<LocalDate> ended = endings.completedAt(residentId).map(ResidencyDayService::dayOf);
        if (ended.isPresent()) {
            return ended;
        }
        Optional<LocalDate> expected = endings.preparingOf(residentId).map(ResidencyEnding::expectedEndDate);
        if (expected.isPresent()) {
            return expected;
        }
        return contracts.ofResident(residentId).stream()
                .filter(c -> c.status() == ContractStatus.TERMINATED)
                .map(c -> contracts.terminatedOn(c.id()))
                .flatMap(Optional::stream)
                .max(Comparator.naturalOrder());
    }

    private Map<LocalDate, AbsentDay> absentDays(long residentId) {
        Map<LocalDate, AbsentDay> byDate = new HashMap<>();
        for (Absence absence : absences.ofResident(residentId)) {
            for (AbsenceDay day : absences.days(absence.id())) {
                byDate.putIfAbsent(day.date(), new AbsentDay(absence.id(), day));
            }
        }
        return byDate;
    }

    private static boolean isDayCare(ContractVersion version) {
        return ResidenceType.DAY_CARE
                .name()
                .equals(version.content().path("residenceType").asString());
    }

    /** Hợp đồng đã ký bắt đầu muộn nhất không sau ngày D; Hiệu lực và Chấm dứt không có cận trên (quá hạn vẫn áp dụng, FR-029; ngày sau mốc dừng vẫn trả kèm dấu), Kết thúc tới ngày kết thúc hiện hành. */
    private Optional<Contract> covering(List<Contract> signed, LocalDate day) {
        return signed.stream()
                .filter(c -> !c.content().startDate().isAfter(day))
                .filter(c -> c.status() != ContractStatus.ENDED || !day.isAfter(lastDay(c)))
                .max(Comparator.comparing((Contract c) -> c.content().startDate())
                        .thenComparing(Contract::id));
    }

    private static LocalDate lastDay(Contract contract) {
        return contract.currentEndDate() == null ? LocalDate.MAX : contract.currentEndDate();
    }

    private static LocalDate dayOf(Instant instant) {
        return instant.atZone(BusinessTime.ZONE).toLocalDate();
    }
}
