package com.eldercare.backend.admission.internal.absence.service;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.internal.absence.persistence.AdmissionDayCareStore;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.domain.ContractVersion;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.roombedallocation.api.dto.DayCareSession;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import com.eldercare.backend.roombedallocation.api.spi.DayCareArrivalCounter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * Đếm lịch đến bán trú cho 003 (tên logic {@code DemLichDenBanTru}; 003 FR-040, research G-11; 004 research T-10): số đăng ký của một
 * (ngày, buổi) là số lịch đến trong phiên bản nội dung hiệu lực tại ngày đó của hợp đồng Hiệu lực, có khung giao {@code [giờ đến, giờ
 * về)} với buổi, trừ người có báo vắng đủ hạn. Không tạo lịch hay bản ghi bán trú. Lịch theo thứ lặp vô hạn nên đếm trong {@value
 * #HORIZON_DAYS} ngày từ {@code fromDate} (đủ mọi thứ trong tuần), hoặc tới lịch theo ngày cụ thể xa nhất (giả định ghi ở plan).
 */
@Component
class M02DayCareArrivalCounter implements DayCareArrivalCounter {

    static final int HORIZON_DAYS = 28;

    private final ContractStore contracts;
    private final AdmissionDayCareStore dayCare;

    M02DayCareArrivalCounter(ContractStore contracts, AdmissionDayCareStore dayCare) {
        this.contracts = contracts;
        this.dayCare = dayCare;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<SessionDate, Integer> countFrom(LocalDate fromDate, List<DayCareSession> sessions) {
        List<Contract> active = activeContracts();
        LocalDate last = fromDate.plusDays(HORIZON_DAYS - 1L);
        for (Contract contract : active) {
            for (JsonNode line : schedule(contract, fromDate)) {
                if (line.hasNonNull("date")) {
                    LocalDate date = LocalDate.parse(line.get("date").asString());
                    last = date.isAfter(last) ? date : last;
                }
            }
        }
        Map<SessionDate, Integer> counts = new HashMap<>();
        for (LocalDate day = fromDate; !day.isAfter(last); day = day.plusDays(1)) {
            for (Contract contract : active) {
                if (!covers(contract, day) || !isDayCareAt(contract, day)) {
                    continue;
                }
                for (JsonNode line : schedule(contract, day)) {
                    if (matches(line, day)) {
                        count(counts, contract.residentId(), day, line, sessions);
                    }
                }
            }
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasFutureArrivals(LocalDate fromDate) {
        for (Contract contract : activeContracts()) {
            for (LocalDate day = fromDate; day.isBefore(fromDate.plusDays(HORIZON_DAYS)); day = day.plusDays(1)) {
                if (covers(contract, day) && isDayCareAt(contract, day)) {
                    for (JsonNode line : schedule(contract, day)) {
                        if (matches(line, day)) {
                            return true;
                        }
                    }
                }
            }
            for (JsonNode line : schedule(contract, fromDate)) {
                if (line.hasNonNull("date")
                        && !LocalDate.parse(line.get("date").asString()).isBefore(fromDate)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void count(
            Map<SessionDate, Integer> counts,
            long residentId,
            LocalDate day,
            JsonNode line,
            List<DayCareSession> sessions) {
        LocalTime arrival = LocalTime.parse(line.get("arrivalTime").asString());
        LocalTime departure = LocalTime.parse(line.get("departureTime").asString());
        for (DayCareSession session : sessions) {
            boolean overlaps =
                    session.active() && session.startTime().isBefore(departure) && arrival.isBefore(session.endTime());
            if (overlaps && !dayCare.noticedAbsence(residentId, day, session.id())) {
                counts.merge(new SessionDate(day, session.id()), 1, Integer::sum);
            }
        }
    }

    private List<Contract> activeContracts() {
        return new ArrayList<>(contracts.list(null, Set.of(ContractStatus.ACTIVE), null));
    }

    private static boolean covers(Contract contract, LocalDate day) {
        return !day.isBefore(contract.content().startDate())
                && (contract.currentEndDate() == null || !day.isAfter(contract.currentEndDate()));
    }

    private boolean isDayCareAt(Contract contract, LocalDate day) {
        return contracts
                .versionAt(contract.id(), day)
                .map(ContractVersion::content)
                .map(content -> ResidenceType.DAY_CARE
                        .name()
                        .equals(content.path("residenceType").asString()))
                .orElse(false);
    }

    private JsonNode schedule(Contract contract, LocalDate day) {
        return contracts
                .versionAt(contract.id(), day)
                .or(() -> contracts.latestVersion(contract.id()))
                .map(version -> version.content().path("arrivalSchedule"))
                .orElse(tools.jackson.databind.node.JsonNodeFactory.instance.arrayNode());
    }

    private static boolean matches(JsonNode line, LocalDate day) {
        boolean weekly = line.hasNonNull("dayOfWeek")
                && line.get("dayOfWeek").asInt() == day.getDayOfWeek().getValue();
        boolean dated = line.hasNonNull("date")
                && LocalDate.parse(line.get("date").asString()).equals(day);
        return weekly || dated;
    }
}
