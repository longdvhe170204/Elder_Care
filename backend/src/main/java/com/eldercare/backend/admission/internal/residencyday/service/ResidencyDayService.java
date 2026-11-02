package com.eldercare.backend.admission.internal.residencyday.service;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.dto.DayCareSessionView;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.admission.internal.absence.domain.Absence;
import com.eldercare.backend.admission.internal.absence.persistence.AbsenceStore;
import com.eldercare.backend.admission.internal.absence.service.AbsenceService;
import com.eldercare.backend.admission.internal.absence.service.DayCareSessions;
import com.eldercare.backend.admission.internal.application.persistence.AdmissionApplicationStore;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.domain.ContractVersion;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.admission.internal.death.persistence.DeathStore;
import com.eldercare.backend.admission.internal.ending.domain.ResidencyEnding;
import com.eldercare.backend.admission.internal.ending.persistence.ResidencyEndingStore;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dữ liệu mọi ngày lưu trú cho 010 (tên logic {@code NgayLuuTruApi}; FR-052, FR-060, FR-066, FR-070, FR-074, FR-075a). Chỉ
 * đọc: mỗi ngày nội trú thuộc một hợp đồng đã ký, có mặt hay vắng theo lượt vắng (đã áp đính chính), đã giải phóng giường hay
 * chưa (Q-395), các dấu "chưa vào ở", "quá hạn hợp đồng" và dấu ngoài thời gian tính phí (sau mốc dừng). Bán trú tính theo buổi ở
 * {@link DayCareSessions}.
 */
@Service
class ResidencyDayService implements ResidencyDayApi {

    private static final Set<ContractStatus> SIGNED =
            Set.of(ContractStatus.ACTIVE, ContractStatus.ENDED, ContractStatus.TERMINATED);

    private final ContractStore contracts;
    private final AbsenceStore absences;
    private final AbsenceService absenceService;
    private final AdmissionApplicationStore applications;
    private final ResidencyEndingStore endings;
    private final DeathStore deaths;
    private final DayCareSessions dayCare;
    private final BusinessTime time;

    ResidencyDayService(
            ContractStore contracts,
            AbsenceStore absences,
            AbsenceService absenceService,
            AdmissionApplicationStore applications,
            ResidencyEndingStore endings,
            DeathStore deaths,
            DayCareSessions dayCare,
            BusinessTime time) {
        this.contracts = contracts;
        this.absences = absences;
        this.absenceService = absenceService;
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
        List<Absence> absent = absences.ofResident(residentId).stream()
                .map(absenceService::effective)
                .toList();
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
            Optional<Absence> away = coveringAbsence(absent, day, today);
            boolean notYetAdmitted =
                    admittedOn.isPresent() ? day.isBefore(admittedOn.get()) : admissionOpen && day.isBefore(today);
            days.add(new ResidencyDayView(
                    day,
                    contract.id(),
                    version.get().id(),
                    away.isEmpty() ? "PRESENT" : "ABSENT",
                    away.map(Absence::id).orElse(null),
                    bedReleasedOn(absent, day, today),
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

    /** Lượt vắng chứa ngày D: từ ngày rời tới ngày về; lượt đang mở tới hôm nay (chưa biết ngày sau); lượt nối tiếp cùng ngày (FR-057) lấy lượt mới nhất. */
    private static Optional<Absence> coveringAbsence(List<Absence> absences, LocalDate day, LocalDate today) {
        return absences.stream()
                .filter(a -> !dayOf(a.departedAt()).isAfter(day))
                .filter(a -> a.returnedAt() != null ? !dayOf(a.returnedAt()).isBefore(day) : !day.isAfter(today))
                .max(Comparator.comparing(Absence::departedAt).thenComparing(Absence::id));
    }

    /** Từ ngày giải phóng giường tới hết lượt vắng không tính phí lưu trú (FR-052, FR-055). */
    private static boolean bedReleasedOn(List<Absence> absences, LocalDate day, LocalDate today) {
        return coveringAbsence(absences, day, today)
                .map(Absence::bedReleasedAt)
                .filter(at -> !dayOf(at).isAfter(day))
                .isPresent();
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
